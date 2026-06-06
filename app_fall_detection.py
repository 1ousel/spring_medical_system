from flask import Flask, request, jsonify
from ultralytics import YOLO
import cv2
import numpy as np
import base64
import os
import sys
import threading
import signal

import speech_service

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

app = Flask(__name__)

MODEL_PATH = os.environ.get(
    "FALL_MODEL_PATH",
    os.path.join(BASE_DIR, "runs", "detect", "fall_detection_model", "weights", "best.pt"),
)
PORT = int(os.environ.get("ALGORITHM_PORT", "5000"))

print(f"Loading YOLO fall detection model from: {MODEL_PATH}")
if not os.path.isfile(MODEL_PATH):
    print(f"WARNING: model file not found: {MODEL_PATH}", file=sys.stderr)
model = YOLO(MODEL_PATH)

inference_lock = threading.Lock()
shutdown_requested = False

os.environ.setdefault("YOLO_VERBOSE", "False")


@app.route("/api/health", methods=["GET"])
def health():
    return jsonify(
        {
            "success": True,
            "status": "ready" if not shutdown_requested else "shutting_down",
            "models": {
                "yolo": model is not None,
            },
            "speech": {
                "ttsEngine": "edge-tts",
            },
            "modelPath": MODEL_PATH,
        }
    )


@app.route("/api/shutdown", methods=["POST"])
def shutdown():
    global shutdown_requested
    shutdown_requested = True

    def _shutdown():
        print("Python algorithm service shutting down...")
        os._exit(0)

    threading.Timer(0.5, _shutdown).start()
    return jsonify({"success": True, "message": "Shutting down"})


def _handle_signal(signum, frame):
    global shutdown_requested
    shutdown_requested = True
    print(f"Received signal {signum}, exiting...")
    os._exit(0)


signal.signal(signal.SIGINT, _handle_signal)
signal.signal(signal.SIGTERM, _handle_signal)
if hasattr(signal, "SIGBREAK"):
    signal.signal(signal.SIGBREAK, _handle_signal)


@app.route("/api/detect", methods=["POST"])
def detect_fall():
    try:
        data = request.json
        if not data or "image" not in data:
            return jsonify({"error": "No image provided"}), 400

        base64_str = data["image"]
        if "," in base64_str:
            base64_str = base64_str.split(",")[1]

        img_bytes = base64.b64decode(base64_str)
        nparr = np.frombuffer(img_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

        if img is None:
            return jsonify({"error": "Failed to decode image"}), 400

        img_h, img_w = img.shape[:2]

        with inference_lock:
            results = model.predict(img, verbose=False)
        result = results[0]

        detections = []
        if result.boxes is not None:
            for box in result.boxes:
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                w = (x2 - x1) / img_w
                h = (y2 - y1) / img_h
                x = x1 / img_w
                y = y1 / img_h
                conf = float(box.conf[0])
                cls = int(box.cls[0])
                name = model.names[cls]
                is_alert = name.lower() == "fall" or cls == 0
                label = "跌倒" if is_alert else "正常/未跌倒"

                detections.append(
                    {
                        "label": label,
                        "x": x,
                        "y": y,
                        "w": w,
                        "h": h,
                        "confidence": conf,
                        "alert": is_alert,
                    }
                )

        return jsonify({"success": True, "detections": detections})
    except Exception as e:
        print(f"Detect Error: {str(e)}", file=sys.stderr)
        return jsonify({"success": False, "error": str(e)}), 500


@app.route("/api/tts", methods=["POST"])
def tts_endpoint():
    try:
        data = request.json or {}
        text = data.get("text")
        if not text or not str(text).strip():
            return jsonify({"success": False, "error": "No text provided"}), 400

        voice = data.get("voice") or speech_service.DEFAULT_TTS_VOICE
        rate = float(data.get("rate", 1.0))

        audio_base64 = speech_service.synthesize_speech_base64(
            str(text).strip(),
            rate=rate,
            voice=voice,
        )
        return jsonify({
            "success": True,
            "audioBase64": audio_base64,
            "text": text,
            "appliedRate": rate,
        })
    except ValueError as e:
        return jsonify({"success": False, "error": str(e)}), 400
    except Exception as e:
        print(f"TTS Error: {str(e)}", file=sys.stderr)
        return jsonify({"success": False, "error": str(e)}), 500


if __name__ == "__main__":
    print(f"Fall detection algorithm service starting on 0.0.0.0:{PORT}")
    app.run(host="0.0.0.0", port=PORT, threaded=True, use_reloader=False)

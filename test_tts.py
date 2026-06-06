import asyncio
import edge_tts

async def test():
    comm = edge_tts.Communicate('测试', 'zh-CN-XiaoxiaoNeural', rate='+50%', pitch='+25Hz', volume='-50%')
    await comm.save('test.mp3')

asyncio.run(test())

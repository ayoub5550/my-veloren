"""my-veloren dev.7 (7.9): launcher icon from Veloren's own app icon.
Usage: python make_icon.py <veloren>/assets/voxygen/net.veloren.veloren.png <out res dir> [preview.png]
Legacy icon = the official square icon; adaptive icon = the "V" cut out of it
(flood fill of the teal panel) on the panel colour, inside the 66dp safe zone."""
import os, sys
from PIL import Image, ImageDraw
src = Image.open(sys.argv[1]).convert('RGBA')
inner = src.crop((22,22,234,234))
# extract the V: drop the teal panel pixels
fg = inner.copy()
px = fg.load()
W,H = fg.size
def is_v(c):
    r,g,b,a = c
    return (r > 80 and g >= r - 5 and b >= r - 5) or (r < 32 and g < 60 and b < 70 and b > r)
from collections import deque
seen=set(); q=deque()
for x in range(W):
    q.append((x,0)); q.append((x,H-1))
for y in range(H):
    q.append((0,y)); q.append((W-1,y))
while q:
    x,y=q.popleft()
    if (x,y) in seen or not (0<=x<W and 0<=y<H): continue
    seen.add((x,y))
    if is_v(px[x,y]): continue
    px[x,y]=(0,0,0,0)
    q.extend([(x+1,y),(x-1,y),(x,y+1),(x,y-1)])
bbox = fg.getbbox(); print("V bbox", bbox)
v = fg.crop(bbox)
BG = (56,119,147,255)
def adaptive_fg(size):
    # 108dp canvas, safe circle 66dp: the V fits in ~58dp
    canvas = Image.new('RGBA',(size,size),(0,0,0,0))
    target = int(size*50/108)
    scale = target/max(v.size)
    vs = v.resize((max(1,round(v.width*scale)), max(1,round(v.height*scale))), Image.NEAREST)
    canvas.paste(vs, ((size-vs.width)//2, (size-vs.height)//2), vs)
    return canvas
out = sys.argv[2]
dens = {'mdpi':1,'hdpi':1.5,'xhdpi':2,'xxhdpi':3,'xxxhdpi':4}
for d,k in dens.items():
    os.makedirs(f"{out}/mipmap-{d}", exist_ok=True)
    src.resize((int(48*k),int(48*k)), Image.LANCZOS if k<5.4 else Image.NEAREST).save(f"{out}/mipmap-{d}/ic_launcher.png")
    adaptive_fg(int(108*k)).save(f"{out}/mipmap-{d}/ic_launcher_foreground.png")
os.makedirs(f"{out}/mipmap-anydpi-v26", exist_ok=True)
open(f"{out}/mipmap-anydpi-v26/ic_launcher.xml","w").write('''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
''')
os.makedirs(f"{out}/values", exist_ok=True)
open(f"{out}/values/colors.xml","w").write('''<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#%02X%02X%02X</color>
</resources>
''' % BG[:3])
# preview: circle mask (MIUI) and squircle
prev = Image.new('RGBA',(3*240,260),(200,160,120,255))
big = Image.new('RGBA',(432,432),BG); f=adaptive_fg(432); big.paste(f,(0,0),f)
for i,shape in enumerate(['circle','rounded','legacy']):
    if shape=='legacy':
        t = src.resize((216,216), Image.LANCZOS)
    else:
        t = big.resize((216,216), Image.LANCZOS)
        m = Image.new('L',(216,216),0); dd=ImageDraw.Draw(m)
        inset = int(216*(108-66*1.25)/108/2)  # launcher shows ~72dp of 108
        inset = 216*18//108
        if shape=='circle': dd.ellipse((inset,inset,216-inset,216-inset),fill=255)
        else: dd.rounded_rectangle((inset,inset,216-inset,216-inset),radius=40,fill=255)
        t.putalpha(m)
    prev.paste(t,(12+i*240,20),t)
prev.save(sys.argv[3]) if len(sys.argv) > 3 else None

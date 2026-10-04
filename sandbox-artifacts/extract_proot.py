import lzma, tarfile, os

d = open('proot.deb', 'rb').read()
assert d[:8] == b'!<arch>\n'
i = 8
found = None
while i < len(d) - 60:
    hdr = d[i:i+60]
    name = hdr[0:16].rstrip(b' ').decode().rstrip('/')
    size = int(hdr[48:58].rstrip())
    body = d[i+60:i+60+size]
    if 'data.tar' in name:
        found = (name, body)
    i += 60 + size + (size % 2)

name, body = found
ext = name.split('.')[-1]
open('data.tar', 'wb').write(lzma.decompress(body) if ext == 'xz' else body)

t = tarfile.open('data.tar')
for m in t.getmembers():
    if m.isfile() and 'proot' in m.name:
        print(f'{m.size:>8}  {m.name}')
        t.extract(m, 'termux-tree')

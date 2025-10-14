import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET

path = Path('Documentacion.docx')
dest = Path('Documentacion.txt')

with zipfile.ZipFile(path) as z:
    xml = z.read('word/document.xml')

root = ET.fromstring(xml)
ns = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
paragraphs = []
for para in root.findall('.//w:p', ns):
    texts = []
    for elem in para.iter():
        tag = elem.tag
        if tag.endswith('}t'):
            texts.append(elem.text or '')
        elif tag.endswith('}br') or tag.endswith('}cr'):
            texts.append('\n')
    if texts:
        paragraphs.append(''.join(texts))

text = '\n'.join(paragraphs)
dest.write_text(text, encoding='utf-8')

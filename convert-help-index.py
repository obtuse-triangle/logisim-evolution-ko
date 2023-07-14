#!/usr/bin/python3

import sys, os, os.path, html, json, re
import xml.etree.cElementTree as ET

from bs4 import BeautifulSoup

def die(msg):
    print("ERROR: " + msg)
    sys.exit(1)

lang = None
home = None

# strip out "Up to", "Back to", and similar navigation links
ignore_tags = {
        "en": [ "Back to ", "Up to ",                  "Next:" ],
        "fr": [ "Back to ", "Voltar à ", "Retornar à", "Next:", "Suivant:" ],
        "es": [ "Back to ", "Retornar à",              "Next:" ],
        "ru": [ "Back to ", "Назад к ",                "Next:", "Далее:" ],
        "el": [ "Back to ",                            "Next:" ],
        "pt": [ "Back to ", "Voltar à ", "Retornar à", "Next:", "Próximo:" ],
        "de": [ "Back to ", "Zurück zur ",             "Next:", "Weiter:" ],
        }

ignore_re = { }
for lang in ignore_tags:
    ignore_re[lang] = [ re.compile('^\s*%s' % tag) for tag in ignore_tags[lang] ]

def ignore(line):
    for regex in ignore_re[lang]:
        if regex.match(line):
            return True
    return False

def html_to_text(html):
    soup = BeautifulSoup(html, features="html.parser")

    # kill all script and style elements
    for script in soup(["script", "style"]):
        script.extract()    # rip it out

    # get text
    text = soup.get_text()

    # break into lines and remove leading and trailing space on each
    lines = (line.strip() for line in text.splitlines())
    # break multi-headlines into a line each
    chunks = (phrase.strip() for line in lines for phrase in line.split("  "))
    # drop blank lines
    text = '\n'.join(chunk for chunk in chunks if chunk and not ignore(chunk))
    return text

# Parse map_en.jhm which looks like...
# <map>
#   <mapID target="xxx" url="yyy">
#   ...
# </map>
# Returns dictionary mapping target to url.
def parse_map_jhm(map_jhm_path):

    print("Parsing %s ..." % (map_jhm_path))

    context = ET.iterparse(map_jhm_path, events=("start", "end"))
    context = iter(context)
    started_map = False
    finished_map = False
    urls = { } # target: url
    depth = 0
    for event, elem in context:
        tag = elem.tag
        value = elem.text
        if not started_map:
            if event == "start" and tag == "map":
                started_map = True
                elem.clear()
                continue
            else:
                die("expecting start of map, but got %s of %s" % (event, tag))
        if finished_map:
            die("expecting end of file, but got %s of %s" % (event, tag))
        if tag == "map":
            if event == "end":
                if depth != 0:
                    die("missing end of mapID, but got %s of %s" % (event, tag))
                finished_map = True
                elem.clear()
                continue
            else:
                die("expecting end of map, but got %s of %s" % (event, tag))
        if event == "start" and tag == "mapID":
            if depth != 0:
                die("expecting end of mapID, but got %s of %s" % (event, tag))
            depth += 1
            attribs = elem.attrib
            target = attribs.get('target')
            url = attribs.get('url')
            if not target:
                die("missing target for mapID")
            if not url:
                die("missing url for mapID")
            # print( "%s [%s] %s" % (("> " * depth), target, text))
            if target in urls:
                die("duplicate mapID target [%s]" % (target))
            urls[target] = "/" + url
            # print("<!-- target [%s] maps to url [%s] -->" % (target, url))
        elif event == "end" and tag == "mapID":
            if depth == 0:
                die("unexpected end of mapID")
            depth -= 1
        else:
            die("unexpected %s of %s" % (event, tag))
        elem.clear()
    
    print("  Found %d target:url pairs." % (len(urls)))

    return urls

class TocItem:
    def __init__(self, parent, attribs, urls):
        self.parent = parent
        self.children = []
        if attribs:
            self.target = attribs.get('target')
            self.title = attribs.get('text')
            self.image = attribs.get('image') # optional
        if not parent:
            self.depth = 0
        else:
            self.depth = parent.depth + 1
            parent.children.append(self)
            if not self.target:
                die("missing target for tocitem")
            if not self.title:
                die("missing title text for tocitem")
            self.url = urls.get(self.target)
            if not self.url:
                print("  WARNING: missing url for target [%s]" % (self.target))
            self.imgurl = None
            if self.image:
                self.imgurl = urls.get(self.image)
                if not self.imgurl:
                    print("  WARNING: missing url for image [%s]" % (self.image))

    def toHTML(self, f):
        n = 0
        if self.parent is not None:
            n += 1
            img = ""
            if self.url:
                link = '<a href="%s">%s</a>' % (self.url, html.escape(self.title))
            else:
                link = html.escape(self.title)
            if self.imgurl:
                f.write('%s<li style="list-style-image: url(%s);">%s</li>\n' %
                        ("  "*self.depth, self.imgurl, link))
            else:
                f.write("%s<li>%s</li>\n" % ("  "*self.depth, link))
            # if len(self.children) == 0:
            #     f.write("</li>\n")
            # else:
            #     f.write("\n")
        if len(self.children) > 0 or self.parent is None:
            f.write("%s<ul>\n" % ("  "*self.depth))
            for child in self.children:
                n += child.toHTML(f)
            f.write("%s</ul>\n" % ("  "*self.depth))
            # if self.parent is not None:
            #     f.write("%s</li>\n" % ("  "*self.depth))
        return n

    def contents(self):
        if not self.url:
            return ""
        path = "%s/doc/%s" % (home, self.url)
        if not os.path.exists(path):
            print("  WARNING: missing %s" % (path))
            return ""
        f = open(path)
        html = f.read()
        f.close()
        return html_to_text(html)
    
    def toJSON(self, f):
        f.write("[\n")
        n = self.toJSONWithoutNewline(f, 0)
        if n > 0:
            f.write("\n")
        f.write("]\n")
        return n

    def toJSONWithoutNewline(self, f, n):
        if self.parent is not None and self.url is not None:
            if n > 0:
                f.write(",\n")
            n += 1
            f.write('  {\n')
            f.write('    "id": %d,\n' % (n))
            f.write('    "title": %s,\n' % (json.dumps(self.title)))
            f.write('    "url": %s,\n' % (json.dumps(self.url)))
            f.write('    "text": %s\n' % (json.dumps(self.contents())))
            f.write('  }')
        for child in self.children:
            n = child.toJSONWithoutNewline(f, n)
        return n

# Parse en/contents.xml which looks like...
# <toc ...>
#   <tocitem target=... text=... image=...>
#     <tocitem target=... text=...>
#     ...
#   </tocitem>
#   ...
# </toc>
# Returns a root TocItem representing the tree.
def parse_contents_xml(contents_xml_path, urls):
    
    print("Parsing %s ..." % (contents_xml_path))

    context = ET.iterparse(contents_xml_path, events=("start", "end"))
    context = iter(context)
    started_toc = False
    finished_toc = False
    root = TocItem(None, None, None)
    cur = root

    for event, elem in context:
        tag = elem.tag
        value = elem.text
        if not started_toc:
            if event == "start" and tag == "toc":
                started_toc = True
                elem.clear()
                continue
            else:
                die("expecting start of toc, but got %s of %s" % (event, tag))
        if finished_toc:
            die("expecting end of file, but got %s of %s" % (event, tag))
        if tag == "toc":
            if event == "end":
                if cur.depth != 0:
                    die("missing end of mapID, but got %s of %s" % (event, tag))
                finished_toc = True
                elem.clear()
                continue
            else:
                die("expecting end of toc, but got %s of %s" % (event, tag))
        if event == "start" and tag == "tocitem":
            attribs = elem.attrib
            node = TocItem(cur, attribs, urls)
            cur = node
        elif event == "end" and tag == "tocitem":
            if cur.parent is None:
                die("unexpected end of tocitem")
            cur = cur.parent
        else:
            die("unexpected %s of %s" % (event, tag))
        elem.clear()

    return root

def generate_sidebar(sidebar_path, tree):
    print("Generating %s ..." % (sidebar_path))
    f = open(sidebar_path, "w")
    n = tree.toHTML(f)
    f.close()
    print("  Wrote %s sidebar entries." % (n))

def generate_sidebar(sidebar_path, tree):
    print("Generating %s ..." % (sidebar_path))
    f = open(sidebar_path, "w")
    n = tree.toHTML(f)
    f.close()
    print("  Wrote %s html sidebar entries." % (n))

def generate_json(json_path, tree):
    print("Generating %s ..." % (json_path))
    f = open(json_path, "w")
    n = tree.toJSON(f)
    f.close()
    print("  Wrote %s json search entries." % (n))

def convert():
    map_jhm_path = "%s/doc/map_%s.jhm" % (home, lang)
    contents_xml_path = "%s/doc/%s/contents.xml" % (home, lang)

    urls = parse_map_jhm(map_jhm_path)
    tree = parse_contents_xml(contents_xml_path, urls)

    sidebar_path = "%s/doc/sidebar_%s.html" % (home, lang)
    generate_sidebar(sidebar_path, tree)

    json_path = "%s/doc/contents_%s.json" % (home, lang)
    generate_json(json_path, tree)

if len(sys.argv) < 2:
    print("usage: %s lang ..." % (sys.argv[0]))
    sys.exit(1)

for lang in sys.argv[1:]:
    print("==== converting help index and search data for lang=%s ====" % (lang))

    home = os.path.dirname(os.path.realpath(sys.argv[0]))

    convert()

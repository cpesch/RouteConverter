#!/usr/bin/env python
import glob, os, sys
from ConfigParser import RawConfigParser, ParsingError
import codecs

locale_parser = RawConfigParser()

def main(path):
    if not os.path.exists(path) or not os.path.isdir(path):
        usage()

    print '==================== ============ =========== =========== =========== ======== ========================='
    print 'LocaleName           LanguageCode LocaleCode2 LocaleCode3 Localeglibc LocaleID LocaleWinName            '
    print '==================== ============ =========== =========== =========== ======== ========================='
    for locale_file in glob.iglob(os.path.join(path, '*.locale')):
        fp = open(locale_file, 'r')
        bom = fp.read(2)
        try:
            if bom == codecs.BOM_UTF16_LE:
                # Having defused the bomb, decode the rest. codecs.open leaves
                # the BOM in for some reason and then RawConfigParser blows up.
                locale_parser.readfp(codecs.getreader('utf_16_le')(fp))
            else:
                fp.seek(0) # False alarm, it wasn't a bomb
                locale_parser.readfp(fp) # Now try reading the whole file
        except ParsingError:
            print "Unable to parse %s!" % os.path.basename(locale_file)[:-7]
        finally:
            fp.close()

        print ' '.join([
            os.path.basename(locale_file)[:-7].ljust(20),
            get_value('LanguageCode'),
            get_value('LocaleCode2'),
            get_value('LocaleCode3'),
            get_value('Localeglibc'),
            get_value('LocaleID'),
            get_value('LocaleWinName', 25),
        ])
    print '==================== ============ =========== =========== =========== ======== ========================='

def get_value(value, padwidth=None):
    if padwidth == None:
        padwidth = len(value)

    return locale_parser.get('PortableApps.comLocaleDetails', value).ljust(padwidth)


def usage():
    print "Usage: %s /path/to/PortableApps/PortableApps.com/App/Locale" % sys.argv[0]
    sys.exit(1)

if __name__ == '__main__':
    if len(sys.argv) != 2:
        usage()

    main(sys.argv[1])

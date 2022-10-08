import bencodepy
import hashlib
import sys

if __name__ == '__main__':

    file_handler = open(sys.argv[1], "rb")

    data_byte = file_handler.read()
    metadata = bencodepy.decode(data_byte)

    print(hashlib.sha1(bencodepy.encode(metadata[b'info'])).hexdigest())

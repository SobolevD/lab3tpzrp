import bencodepy
import sys


def parse_peers(bytes):
    peers_count = int(len(bytes) / 6)
    peers = []

    for i in range(0, peers_count):
        peers.append(str(bytes[i * 6]) + '.' +
                     str(bytes[i * 6 + 1]) + '.' +
                     str(bytes[i * 6 + 2]) + '.' +
                     str(bytes[i * 6 + 3]) + ':' +
                     str(int(f'{bytes[i * 6 + 4]:x}' + f'{bytes[i * 6 + 5]:x}', 16)))
    return ';'.join(peers)


if __name__ == '__main__':

    file_handler = open(sys.argv[1], "rb")

    data_byte = file_handler.read()
    metadata = bencodepy.decode(data_byte)

    peers_bytes = metadata[b'peers']
    print(f'{parse_peers(peers_bytes)}')


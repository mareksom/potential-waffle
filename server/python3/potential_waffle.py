#!/usr/bin/python3

from netifaces import interfaces, ifaddresses, AF_INET
from termcolors import *
import dir_descriptor
import os
import socket
import sys

NUMBER_OF_INTEGER_BYTES = 4
MAX_INTEGER = 2**32 - 1
BYTE_ORDER = 'big'
GET_DESCRIPTOR_COMMAND = 1145393930  # b'DES\n'
GET_FILE_COMMAND = 1179208714 # b'FIL\n'
MAX_PATH_LENGTH = 4096

FILE_CHUNK_SIZE = 4096


if len(sys.argv) == 0:
  program_name = 'potential-waffle'
else:
  program_name = sys.argv[0]

if len(sys.argv) != 2:
  print(RED("Usage: {} root-directory".format(program_name)))
  sys.exit(1)

root_directory = sys.argv[1]

if not os.path.isdir(root_directory):
  print(RED("'{}' is not a valid directory.".format(root_directory)))
  sys.exit(1)

sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_address = ('0.0.0.0', 0)
sock.bind(server_address)

sock.listen(1)

def ip4_addresses():
  ip_list = []
  for interface in interfaces():
    try:
      for link in ifaddresses(interface)[AF_INET]:
        ip_list.append("({}) {}".format(interface, link['addr']))
    except KeyError:
      pass
  return ip_list

# Reads exactly n bytes from the connection.
def read_n_bytes(connection, n):
  b = b''
  while len(b) < n:
    new_bytes = connection.recv(n - len(b))
    b += new_bytes
    if len(new_bytes) == 0:
      raise EOFError("Got EOF")
  return b
# Reads an integer from the connection.
def read_integer(connection):
  int_bytes = read_n_bytes(connection, NUMBER_OF_INTEGER_BYTES)
  return int.from_bytes(int_bytes, byteorder=BYTE_ORDER)
# Writes an integer to the connection.
def write_integer(connection, i):
  int_bytes = i.to_bytes(NUMBER_OF_INTEGER_BYTES, byteorder=BYTE_ORDER)
  connection.sendall(int_bytes)

def write_file(connection, path):
  with open(path, 'rb') as f:
    f.seek(0, os.SEEK_END)
    file_size = f.tell()
    f.seek(0)
    write_integer(connection, file_size)
    if file_size > 1024 * 1024 * 1024:
      print(RED("File's size is over 1GiB."))
      return
    while True:
      chunk = f.read(FILE_CHUNK_SIZE)
      if len(chunk) == 0:
        break
      while len(chunk) > 0:
        bytes_sent = connection.send(chunk)
        chunk = chunk[bytes_sent:]
    print(GREEN("File was sent successfully."))

def is_file_under_directory(file, directory):
  return os.path.realpath(file).startswith(os.path.realpath(directory))

while True:
  print(BLUE(("Waiting for a connection on {} port " + UNDERLINED("{}") + ".")
      .format(ip4_addresses(), sock.getsockname()[1])))
  try:
    connection, client_address = sock.accept()
  except KeyboardInterrupt:
    print(BLUE("\nGoodbye!"))
    break
  print(GREEN("Connected with {} on port {}."
      .format(*connection.getpeername())))
  try:
    # Processes all the commands.
    while True:
      command = read_integer(connection)
      if command == GET_DESCRIPTOR_COMMAND:
        print(BLUE("Got descriptor command."))
        descriptor_bytes = dir_descriptor.get_descriptor(root_directory)
        # Writes number of bytes of the descriptor.
        write_integer(connection, len(descriptor_bytes))
        # Writes the descriptor.
        connection.sendall(descriptor_bytes)
      elif command == GET_FILE_COMMAND:
        print(BLUE("Got file command."))
        path_length = read_integer(connection)
        if path_length > MAX_PATH_LENGTH:
          print(RED("The path is too long: {}".format(path_length)))
          break
        path = os.path.join(
            root_directory,
            read_n_bytes(connection, path_length).decode('utf-8'))
        print(BLUE("Requested path: {}.".format(repr(path))))
        if not is_file_under_directory(path, root_directory):
          print(RED("Requested file is outside of the root directory."))
          break
        if os.path.isfile(path):
          write_file(connection, path)
        else:
          write_integer(connection, MAX_INTEGER)
          print(YELLOW("File doesn't exist."))
      else:
        print(RED("Unknown command: {}.".format(command)))
  except EOFError:
    print(YELLOW("The client ended transmission."))
  except UnicodeError:
    print(RED("Unicode error."))
  finally:
    connection.close()

import hashlib

def compute(path):
  hash = hashlib.md5()
  with open(path, "rb") as file:
    for chunk in iter(lambda: file.read(4069), b""):
      hash.update(chunk)
  return hash.hexdigest()

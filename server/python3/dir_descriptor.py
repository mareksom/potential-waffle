from termcolors import *
import checksum
import math
import os
import xml.etree.ElementTree

class DirDescriptorException(Exception):
  pass

def _check_isdir(root_directory):
  if not os.path.isdir(root_directory):
    raise DirDescriptorException(
        "'{}' is not a valid directory.".format(root_directory))

# Returns a list of all files, the returned paths are relative to the root
# directory.
def _list_files_including_hidden_directories(root_directory):
  _check_isdir(root_directory)
  for subdir, dirs, files in os.walk(root_directory):
    for file in files:
      if not file.startswith('.'):
        # The file is not hidden.
        yield os.path.relpath(os.path.join(subdir, file), root_directory)

def _list_files(root_directory):
  for path in _list_files_including_hidden_directories(root_directory):
    is_hidden = False
    p = path
    while p:
      head, tail = os.path.split(p)
      if tail.startswith('.'):
        is_hidden = True
        break
      p = head
    if not is_hidden:
      yield path

def _get_cache_file_name(root_directory):
  return os.path.join(root_directory, '.potential-waffle')

# Returns as much information about files as possible.
# Returns a dictionary: "filename" -> {"checksum" -> ???, "timetamp" -> ???}.
def _get_cached_file_info(root_directory):
  _check_isdir(root_directory)
  descriptor_file_path = _get_cache_file_name(root_directory)
  if not os.path.isfile(descriptor_file_path):
    return {}
  result = {}
  try:
    xml_tree = xml.etree.ElementTree.parse(descriptor_file_path).getroot()
  except xml.etree.ElementTree.ParseError:
    return {}
  for file_element in xml_tree.findall('file'):
    filename = file_element.get('path')
    checksum_ = file_element.get('checksum')
    timestamp = file_element.get('timestamp')
    if filename is not None and \
        checksum_ is not None and \
        timestamp is not None:
      try:
        result[filename] = {"checksum": checksum_, "timestamp": int(timestamp)}
      except ValueError:
        pass
  return result

# Takes the dictionary: "filename" -> {"checksum" -> ???, "timestamp" -> ???}
# and saves it in the cache file.
def _save_cached_file_info(root_directory, info):
  _check_isdir(root_directory)
  xml_tree = xml.etree.ElementTree.Element('descriptor')
  for filename, data in info.items():
    file_element = xml.etree.ElementTree.Element('file')
    file_element.set("path", filename)
    file_element.set("checksum", data["checksum"])
    file_element.set("timestamp", str(data["timestamp"]))
    xml_tree.append(file_element)
  xml.etree.ElementTree.ElementTree(xml_tree) \
      .write(_get_cache_file_name(root_directory), encoding='utf-8')

# Returns last modification time of the file as a single integer: number of
# seconds since the epoch.
def _last_modification_time(file_path):
  return math.floor(os.path.getmtime(file_path))

# Returns a dictionary: "filename" -> {"checksum" -> ???, "timestamp" -> ???}.
def _get_current_file_info(root_directory):
  _check_isdir(root_directory)
  cached_info = _get_cached_file_info(root_directory)
  result = {}
  for filename in _list_files(root_directory):
    full_path = os.path.join(root_directory, filename)
    # Checks whether the file checksum has to be recalculated.
    update_checksum = False
    if filename in cached_info:
      cache_time = cached_info[filename]["timestamp"]
      last_modification_time = _last_modification_time(full_path)
      if cache_time < last_modification_time:
        update_checksum = True
    else:
      update_checksum = True
    # Recalculates the checksum if necessary.  Adds the file to the result.
    if update_checksum:
      last_modification_time = _last_modification_time(full_path)
      checksum_ = None
      while True:
        print(YELLOW("Recalculating checksum of '{}'".format(filename)))
        checksum_ = checksum.compute(full_path)
        last_modification_time2 = _last_modification_time(full_path)
        if last_modification_time == last_modification_time2:
          break
        else:
          last_modification_time = last_modification_time2
      result[filename] = \
          {"checksum": checksum_, "timestamp": last_modification_time}
    else:
      result[filename] = cached_info[filename]
  _save_cached_file_info(root_directory, result)
  return result

# Returns XML bytes representing all the files and their checksums.
def get_descriptor(root_directory):
  _check_isdir(root_directory)
  file_info = _get_current_file_info(root_directory)
  xml_tree = xml.etree.ElementTree.Element('descriptor')
  for filename, data in file_info.items():
    file_element = xml.etree.ElementTree.Element('file')
    file_element.set("path", filename)
    file_element.set("checksum", data["checksum"])
    xml_tree.append(file_element)
  return xml.etree.ElementTree.tostring(xml_tree, encoding='utf-8')

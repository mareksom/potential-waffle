package pl.mareksom.potentialwaffle;

import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class DirDescriptor {
    public FileDescriptor getFileDescriptor(String filename) {
        return fileInfo_.get(filename);
    }

    public String[] getAllFilenames() {
        return fileInfo_.keySet().toArray(new String[0]);
    }

    public Patch computePatchTo(DirDescriptor goal) {
        Patch patch = new Patch();
        for (Map.Entry<String, FileDescriptor> entry : fileInfo_.entrySet()) {
            String filename = entry.getKey();
            FileDescriptor fdSource = entry.getValue();
            FileDescriptor fdGoal = goal.getFileDescriptor(filename);
            if (fdGoal == null) {
                patch.addFileToDelete(filename);
            } else if (!fdSource.checksum().equals(fdGoal.checksum())) {
                patch.addFileToUpdate(filename);
            }
        }
        for (String filename : goal.getAllFilenames()) {
            if (getFileDescriptor(filename) == null) {
                patch.addFileToCopy(filename);
            }
        }
        return patch;
    }

    public static DirDescriptor fromInputStream(InputStream stream) throws DescriptorException {
        try {
            DirDescriptor result = new DirDescriptor();
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.parse(stream);
            NodeList elements = document.getElementsByTagName("file");
            for (int i = 0; i < elements.getLength(); i++) {
                Node node = elements.item(i);
                NamedNodeMap attributes = node.getAttributes();
                Node filenameNode = attributes.getNamedItem("path");
                Node checksumNode = attributes.getNamedItem("checksum");
                Node timestampNode = attributes.getNamedItem("timestamp");
                if (filenameNode != null && checksumNode != null) {
                    String filename = filenameNode.getNodeValue();
                    String checksum = checksumNode.getNodeValue();
                    String timestampString = "0";
                    if (timestampNode != null) {
                        timestampString = timestampNode.getNodeValue();
                    }
                    try {
                        long timestamp = Long.parseLong(timestampString);
                        result.fileInfo_.put(filename, new FileDescriptor(checksum, timestamp));
                    } catch (NumberFormatException e) {
                        // Nothing to do.
                    }
                }
            }
            return result;
        } catch (ParserConfigurationException|SAXException|IOException e) {
            throw new DescriptorException(e.getMessage());
        }
    }

    public static DirDescriptor create(File directory, StatusUpdater statusUpdater) throws DescriptorException {
        DirDescriptor result = new DirDescriptor();
        statusUpdater.setIndeterminate(true, "Retrieving file information from cache.");
        DirDescriptor cache_info = getCachedFileInfo_(directory);
        statusUpdater.setIndeterminate(true, "Scanning the directory tree.");
        boolean isAnythingNew = false;
        for (File file : listFiles_(directory)) {
            String relpath = getRelativePath_(directory, file);
            FileDescriptor fd = cache_info.getFileDescriptor(relpath);
            boolean computeChecksum = false;
            if (fd == null) {
                statusUpdater.log("New file detected: '" + relpath + "'.\n");
                computeChecksum = true;
            } else {
                long lastModified = file.lastModified();
                if (fd.timestamp() < lastModified) {
                    statusUpdater.log("Update detected: '" + relpath + "'.\n");
                    computeChecksum = true;
                }
            }
            if (computeChecksum) {
                isAnythingNew = true;
                long lastModified = file.lastModified();
                String checksum = null;
                while (true) {
                    try {
                        checksum = Md5Checksum.getMd5Checksum(file, relpath, statusUpdater);
                        statusUpdater.setIndeterminate(true, "Scanning the directory tree.");
                        long lastModified2 = file.lastModified();
                        if (lastModified == lastModified2) {
                            break;
                        } else {
                            lastModified = lastModified2;
                        }
                    } catch (Exception e) {
                        throw new DescriptorException("Cannot compute md5 checksum: " + e.getMessage());
                    }
                }
                result.fileInfo_.put(relpath, new FileDescriptor(checksum, lastModified));
            } else {
                result.fileInfo_.put(relpath, fd);
            }
        }
        if (!isAnythingNew) {
            statusUpdater.log("Directory descriptor is up to date.\n");
        } else {
            saveCachedFileInfo_(directory, result);
            statusUpdater.log("Directory descriptor is updated.\n");
        }
        statusUpdater.setIndeterminate(false, "Directory tree is scanned.");
        return result;
    }

    private DirDescriptor() {
        fileInfo_ = new HashMap<>();
    }

    private static List<File> listFiles_(File directory) throws DescriptorException {
        if (!directory.exists() || !directory.isDirectory()) {
            throw new DescriptorException("Not a directory.");
        }
        List<File> list = new ArrayList<>();
        for (String filename : directory.list()) {
            File child = new File(directory, filename);
            if (!child.isHidden()) {
                if (child.isDirectory()) {
                    list.addAll(listFiles_(child));
                } else if (child.isFile()) {
                    list.add(child);
                }
            }
        }
        return list;
    }

    private static String getRelativePath_(File directory, File file) {
        return directory.toURI().relativize(file.toURI()).getPath();
    }

    private static DirDescriptor getCachedFileInfo_(File directory) throws DescriptorException {
        File descriptor_file = getCacheFile_(directory);
        if (!descriptor_file.exists() || !descriptor_file.isFile() || !descriptor_file.canRead()) {
            return new DirDescriptor();
        }
        try {
            DirDescriptor result = fromInputStream(new FileInputStream(descriptor_file));
            return result;
        } catch (FileNotFoundException e) {
            throw new DescriptorException("Descriptor file not found (but it was there a second ago...).");
        }
    }

    private static void saveCachedFileInfo_(
            File directory, DirDescriptor info) throws DescriptorException {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element xmlTree = document.createElement("descriptor");
            for (Map.Entry<String, FileDescriptor> entry : info.fileInfo_.entrySet()) {
                String filename = entry.getKey();
                FileDescriptor fd = entry.getValue();
                Element element = document.createElement("file");
                element.setAttribute("path", filename);
                element.setAttribute("checksum", fd.checksum());
                element.setAttribute("timestamp", Long.toString(fd.timestamp()));
                xmlTree.appendChild(element);
            }
            document.appendChild(xmlTree);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource domSource = new DOMSource(document);
            FileOutputStream fos = new FileOutputStream(getCacheFile_(directory));
            StreamResult streamResult = new StreamResult(fos);
            transformer.transform(domSource, streamResult);
        } catch (FileNotFoundException|ParserConfigurationException|TransformerException e) {
            throw new DescriptorException("Hym? " + e.getMessage());
        }
    }

    private static File getCacheFile_(File directory) {
        return new File(directory, ".potential-waffle");
    }

    private Map<String, FileDescriptor> fileInfo_;
}

package com.android.timezone.distro;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

/* loaded from: classes2.dex */
public final class FileUtils {
    private FileUtils() {
    }

    public static File createSubFile(File parentDir, String name) throws IOException {
        File subFile = new File(parentDir, name).getCanonicalFile();
        if (!subFile.getPath().startsWith(parentDir.getCanonicalPath())) {
            throw new IOException(name + " must exist beneath " + parentDir + ". Canonicalized subpath: " + subFile);
        }
        return subFile;
    }

    public static void ensureDirectoriesExist(File dir, boolean makeWorldReadable) throws IOException {
        LinkedList<File> dirs = new LinkedList<>();
        File currentDir = dir;
        do {
            dirs.addFirst(currentDir);
            currentDir = currentDir.getParentFile();
        } while (currentDir != null);
        Iterator<File> it = dirs.iterator();
        while (it.hasNext()) {
            File dirToCheck = it.next();
            if (!dirToCheck.exists()) {
                if (!dirToCheck.mkdir()) {
                    throw new IOException("Unable to create directory: " + dir);
                } else if (makeWorldReadable) {
                    makeDirectoryWorldAccessible(dirToCheck);
                }
            } else if (!dirToCheck.isDirectory()) {
                throw new IOException(dirToCheck + " exists but is not a directory");
            }
        }
    }

    public static void makeDirectoryWorldAccessible(File directory) throws IOException {
        if (!directory.isDirectory()) {
            throw new IOException(directory + " must be a directory");
        }
        makeWorldReadable(directory);
        if (!directory.setExecutable(true, false)) {
            throw new IOException("Unable to make " + directory + " world-executable");
        }
    }

    public static void makeWorldReadable(File file) throws IOException {
        if (!file.setReadable(true, false)) {
            throw new IOException("Unable to make " + file + " world-readable");
        }
    }

    public static void rename(File from, File to) throws IOException {
        ensureFileDoesNotExist(to);
        if (!from.renameTo(to)) {
            throw new IOException("Unable to rename " + from + " to " + to);
        }
    }

    public static void ensureFileDoesNotExist(File file) throws IOException {
        if (file.exists()) {
            if (!file.isFile()) {
                throw new IOException(file + " is not a file");
            }
            doDelete(file);
        }
    }

    public static void doDelete(File file) throws IOException {
        if (!file.delete()) {
            throw new IOException("Unable to delete: " + file);
        }
    }

    public static boolean isSymlink(File file) throws IOException {
        String baseName = file.getName();
        String canonicalPathExceptBaseName = new File(file.getParentFile().getCanonicalFile(), baseName).getPath();
        return !file.getCanonicalPath().equals(canonicalPathExceptBaseName);
    }

    public static void deleteRecursive(File toDelete) throws IOException {
        File[] listFiles;
        if (toDelete.isDirectory()) {
            for (File file : toDelete.listFiles()) {
                if (file.isDirectory() && !isSymlink(file)) {
                    deleteRecursive(file);
                } else {
                    doDelete(file);
                }
            }
            String[] remainingFiles = toDelete.list();
            if (remainingFiles.length != 0) {
                throw new IOException("Unable to delete files: " + Arrays.toString(remainingFiles));
            }
        }
        doDelete(toDelete);
    }

    public static boolean filesExist(File rootDir, String... fileNames) {
        for (String fileName : fileNames) {
            File file = new File(rootDir, fileName);
            if (!file.exists()) {
                return false;
            }
        }
        return true;
    }

    public static byte[] readBytes(File file, int maxBytes) throws IOException {
        if (maxBytes <= 0) {
            throw new IllegalArgumentException("maxBytes ==" + maxBytes);
        }
        FileInputStream in = new FileInputStream(file);
        try {
            byte[] max = new byte[maxBytes];
            int bytesRead = in.read(max, 0, maxBytes);
            byte[] toReturn = new byte[bytesRead];
            System.arraycopy(max, 0, toReturn, 0, bytesRead);
            in.close();
            return toReturn;
        } catch (Throwable th) {
            try {
                in.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public static void createEmptyFile(File file) throws IOException {
        new FileOutputStream(file, false).close();
    }
}
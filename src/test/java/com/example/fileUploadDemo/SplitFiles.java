package com.example.fileUploadDemo;

import org.springframework.util.DigestUtils;
import org.thymeleaf.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SplitFiles {
    private static final String dir = "E:\\tmp\\";
    private static final String suffix = "";

    /**
     * 文件切片
     *
     * @param fileName
     * @param mBperSplit
     * @throws IOException
     */
    public static List<Path> splitFile(final String fileName, final int mBperSplit) throws IOException {

        if (mBperSplit <= 0) {
            throw new IllegalArgumentException("mBperSplit must be more than zero");
        }

        List<Path> partFiles = new ArrayList<>();
        final long sourceSize = Files.size(Paths.get(fileName));
        final long bytesPerSplit = 1024L * 1024L * mBperSplit;
        final long numSplits = sourceSize / bytesPerSplit;
        final long remainingBytes = sourceSize % bytesPerSplit;
        int position = 0;
        int index = 0;

        try (RandomAccessFile sourceFile = new RandomAccessFile(fileName, "r");
             FileChannel sourceChannel = sourceFile.getChannel()) {

            for (; position < numSplits; position++, index++) {
                //write multipart files.
                writePartToFile(bytesPerSplit, position * bytesPerSplit, index, sourceChannel, partFiles);
            }

            if (remainingBytes > 0) {
                writePartToFile(remainingBytes, position * bytesPerSplit, index, sourceChannel, partFiles);
            }
        }
        return partFiles;
    }

    private static void writePartToFile(long byteSize, long position, int index, FileChannel sourceChannel, List<Path> partFiles) throws IOException {
        Path fileName = Paths.get(dir + index + suffix);
        try (RandomAccessFile toFile = new RandomAccessFile(fileName.toFile(), "rw");
             FileChannel toChannel = toFile.getChannel()) {
            sourceChannel.position(position);
            toChannel.transferFrom(sourceChannel, 0, byteSize);
        }
        partFiles.add(fileName);
    }

    public static String getFileMd5(File file){
        String md5Hex = "";
        try (FileInputStream inputStream = new FileInputStream(file);
        ){// 得到文件MD5
            md5Hex = DigestUtils.md5DigestAsHex(inputStream);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return md5Hex;
    }

}

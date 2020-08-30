package com.example.fileUploadDemo.controller;

import com.example.fileUploadDemo.entity.Resp;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.thymeleaf.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

@RestController
@CrossOrigin
@Api(tags = "文件接口")
public class FileController {
    Logger logger = LoggerFactory.getLogger(getClass());

    @Value("${upload.path:/server/uploads/}")
    private String uploadDir;
    private List<File> chunkFileList;
    private File mergedFile;

    @ApiOperation(value ="查询文件", notes = "根据文件名和查询文件是否存在或通过MD5查询目标路径下分片上传进度")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "fileName",
                    value = "文件名", required = true),
            @ApiImplicitParam(paramType = "query", name = "fileMd5Value",
                    value = "MD5", required = true)
    })
    @GetMapping("/check/file")
    public Resp checkfile(@RequestParam("fileName") String fileName,
                          @RequestParam("fileMd5Value") String fileMd5Value) {
        //文件路径
        File filePath = new File(uploadDir, fileName);
        //分片文件夹路径
        File folderPath = new File(uploadDir, fileMd5Value);

        if (filePath.exists()) {
            // 如果文件已经存在, 不用再继续上传, 真接秒传
            com.example.fileUploadDemo.entity.File file = new com.example.fileUploadDemo.entity.File(true, uploadDir + fileName);
            return new Resp(1, file, "file is exist");
        }

        List<String> chunkList = new ArrayList<>();

        if (folderPath.exists()) {
            // 如果文件夹(md5值后的文件)存在, 就获取已经上传的块
            logger.info("文件夹路径: " + folderPath);
            File[] fileList = folderPath.listFiles();
            for (File f : fileList) {
                if (f.isFile()) {
                    logger.info("文件" + fileName + "已有分片" + f.getName());
                    chunkList.add(f.getName());
                }
            }
        }
        return new Resp(1, chunkList, "folder list");
    }

    @ApiOperation(value ="文件上传", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "total",
                    value = "分片总数", required = true),
            @ApiImplicitParam(paramType = "query", name = "index",
                    value = "分片序号", required = true),
            @ApiImplicitParam(paramType = "query", name = "fileMd5Value",
                    value = "MD5", required = true)
    })
    @PostMapping("/upload")
    public Resp uploadFile(@RequestParam("total") String total,
                           @RequestParam("index") String index,
                           @RequestParam("fileMd5Value") String fileMd5Value,
                           MultipartFile data){
        logger.info("开始接收文件分片" + index + " 保存路径: " + fileMd5Value);
        //分片文件夹
        File folder = new File(uploadDir + fileMd5Value);
        if (!folder.exists()){
            folder.mkdirs();
        }
        try{
            data.transferTo(new File(folder, index));
            logger.info("已接收当前分片 " + index);
            if (Integer.parseInt(total) == Integer.parseInt(index) + 1){
                logger.info("已接收全部分片 "+ "保存路径: " + fileMd5Value);
            }
            return new Resp(1, index);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //分片上传失败
        logger.info("分片" + index + "上传失败");
        return new Resp(0, "Error");
    }

    @ApiOperation(value ="文件合成", notes = "")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", name = "md5",
                    value = "md5", required = true),
            @ApiImplicitParam(paramType = "query", name = "fileName",
                    value = "文件名", required = true),
            @ApiImplicitParam(paramType = "query", name = "size",
                    value = "文件大小", required = true)
    })
    @GetMapping("/merge")
    public Resp merge(@RequestParam("md5") String md5,
                      @RequestParam("fileName") String fileName){

        //1.合并分片文件
        logger.info("开始合并文件... md5: "+ md5 +" fileName: "+ fileName);
        File folder = new File(uploadDir + md5);
        File mergedFile = new File(uploadDir + fileName);
        File[] files = folder.listFiles();
        List<File> fileList = Arrays.asList(files);
        // 开始合并
        mergedFile = this.mergeFile(fileList, mergedFile);
        if (mergedFile == null){
            logger.info("合并文件失败");
            return new Resp(0, "merge failed");
        }
        // 2. 校验文件MD5是否与前端传入一致
        boolean checkResult = this.checkFileMd5(mergedFile, md5);
        // 校验失败
        if (!checkResult) {
            logger.info("md5校验失败");
            return new Resp(0, "md5 not match");
        }
        logger.info(fileName + "文件合并完成");
        return new Resp(1);
    }

    /**
     * 合并文件
     * @param chunkFileList
     * @param mergedFile
     * @return
     */
    private File mergeFile(List<File> chunkFileList, File mergedFile) {
        try {
            if (chunkFileList == null || chunkFileList.size() < 1) {
                return null;
            }
            // 有删 无创建
            if (mergedFile.exists()) {
                mergedFile.delete();
            } else {
                mergedFile.createNewFile();
            }
            // 排序
            Collections.sort(chunkFileList, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    if (Integer.parseInt(f1.getName()) > Integer.parseInt(f2.getName())) {
                        return 1;
                    }
                    return -1;
                }
            });

            byte[] b = new byte[1024];
            RandomAccessFile writeFile = new RandomAccessFile(mergedFile, "rw");
            for (File chunkFile : chunkFileList) {
                RandomAccessFile readFile = new RandomAccessFile(chunkFile, "r");
                int len = -1;
                while ((len = readFile.read(b)) != -1) {
                    writeFile.write(b, 0, len);
                }
                readFile.close();
            }
            writeFile.close();
            return mergedFile;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 检查文件 MD5 是否匹配
     * @param mergedFile
     * @param md5
     * @return
     */
    private boolean checkFileMd5(File mergedFile, String md5){

        try (
            FileInputStream inputStream = new FileInputStream(mergedFile);
            ){// 得到文件MD5
            String md5Hex = DigestUtils.md5DigestAsHex(inputStream);

            if (StringUtils.equalsIgnoreCase(md5, md5Hex)) {
                return true;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}

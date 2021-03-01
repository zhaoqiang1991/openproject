package com.example.plugin;

import com.android.build.gradle.api.BaseVariant;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileTree;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.specs.Spec;
import org.gradle.api.specs.internal.ClosureSpec;
import org.gradle.api.tasks.Input;
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream;

public class FindServiceAction implements Action<DefaultTask> {
    static final String SERVICE_DIRECTION = "META-INF/services/serviceloader/";
    static final String USR_SUFFIX = "_use";
    static final String AUTO_SUFFIX = "_auto";

    @Input
    Project project;//项目工程
    @Input
    BaseVariant variant;//变体
    @Input
    ServiceLoaderExtension extension;
    @Input
    FileCollection fileCollection;//所有文件操作集合
    //收集方法，存储方法
    Map<String, Map<String, String>> servicesMap = new HashMap<>();
    Map<String, Set<String>> useServiceMap = new HashMap<>();
    Map<String, Map<String, Set<String>>> useFilePathMap = new HashMap<>();
    List<String> classNameList = new ArrayList<>();


    @Override
    public void execute(DefaultTask defaultTask) {
        if (fileCollection == null || fileCollection.isEmpty()) return;
        long startTime = System.currentTimeMillis();
        for (File outputFile : fileCollection.getFiles()) {
            String rootPath = outputFile.getPath();
            FileTree tree = project.fileTree(dir: outputFile);
            tree.filter {
                File file ->
                    return isMetaServices(rootPath, file) || file.getName().endsWith(".jar");

            }.each {
                File file ->
                    if (file.name.endsWith(".jar")) {
                        processJar(file);
                    } else if (file.name.endsWith(USR_SUFFIX)) {
                        if (extension.enableLog) {
                            println("-----use serviceloader process meta file:${file.path}")
                        }
                        Set<String> set = new HashSet<>();
                        file.eachLine {
                            String line ->
                                if (line != null && line.length() > 0) {
                                    //路径
                                    set.add(line);
                                }
                        }
                        if (set.isEmpty()) {
                            addUserService(file.name, set, file.path);
                            file.delete();
                        }

                    } else {
                        if (extension.enableLog) {
                            println("----serviceLoader process meta file ${file.path}")
                        }
                        Map<String, String> map = new HashMap<>();
                        file.eachLine {
                            String line ->
                                processLine(line, map);
                        }

                        if (map.isEmpty()) {
                            addService(file.name, map);
                            file.delete();
                        }
                    }
            }
        }
        def compileTask = defaultTask.project.tasks.findByName("compile${variant.name.capitalize()}" + "JavaWithJavac")
        String classesFolderName = "$compileTask.destinationDir.path"
        if (extension.enableLog) {
            println("---service loader classFolderName;${classesFolderName}");
        }
        String serviceFolderName = "${classesFolderName}" + "/" + SERVICE_DIRECTION
        FileTree serviceTree = project.fileTree(dir: serviceFolderName);
        serviceTree.each {
            File file ->
                if (file.name.endsWith(USR_SUFFIX)) {
                    if (extension.enableLog) {
                        println("--use serviceFolderName:${file.path}")
                    }
                    Set<String> set = new HashSet<>();
                    file.eachLine {
                        String line ->
                            if (line != null && line.length() > 0) {
                                set.add(line);
                            }
                    }
                    if (!set.isEmpty()) {
                        addUserService(file.name, set, file.path);
                        file.delete();
                    }
                } else {
                    if (extension.enableLog) {
                        println("----serviceFolderName${file.path}")
                    }
                    Map<String, String> map = new HashMap<>();
                    file.eachLine {
                        String line ->
                            processLine(line, map);
                    }
                    if (!map.isEmpty()) {
                        addService(file.name, map);
                        file.delete();
                    }
                }
        }
        checkServices()
        def mergeAssetTask = defaultTask.project.tasks.getByName("merge${variant.name.capitalize()}Assets")
        String assetFolderName = "${mergeAssetTask.outputDir.path}"
        if (extension.enableLog) {
            println("----serviceLoader assetsFolderName${assetFolderName}")
        }
        File file = new File("${assetFolderName}/serviceloader/services")
        //TODO 已经解决
        //file.mkdirs()//mkdirs(file.getParentFile());
        file.parentFile.mkdirs();
        if (file.exists()) {
            file.delete();
        }
        if (servicesMap.isEmpty()) {
            return
        }
        if (extension.enableLog) {
            //开始写入文件
            println("-----serviceLoader writes service into${file.path}")
        }
        def printWriter = file.newPrintWriter();
        classNameList.clear();
        servicesMap.each {
            String key, Map<String, String> value ->
                classNameList.add(key);
                StringBuilder strBuilder = new StringBuilder();
                value.each { entry ->
                    classNameList.add(entry.value)
                    strBuilder.append("[$entry.key,$entry.value]")
                }
                printWriter.write("$key:${strBuilder.toString()}\n");
        }
        printWriter.flush();
        printWriter.close();
        println("-----serviceloader get service :${(System.currentTimeMillis() - startTime) / 1000}s")
    }

    private void checkServices() {
        if (servicesMap.isEmpty() || useServiceMap.isEmpty()) return;
        useServiceMap.each {
            String interfaceName, Set<String> keySet ->
                Map<String, String> map = servicesMap.get(interfaceName);
                if (map == null || map.isEmpty()) {
                    throw new GradleException("The interface ${interfaceName} " + "you are using does not exit" +
                            "!\n error use service,file path:\n" + userFilePath(interfaceName, null))
                }
                keySet.each { key ->
                    String service = map.get(key);
                    if (service == null || service.isEmpty()) {
                        throw new GradleException("The interface${interfaceName}" + "you ars using exitsted," +
                                "but service class file path\n" + userFilePath(interfaceName, key))
                    }
                }
        }
    }

    private String userFilePath(String interfaceName, String key) {
        if (useFilePathMap.isEmpty() || interfaceName == null) return ""
        Map<String, Set<String>> keyPathMap = useFilePathMap.get(interfaceName)
        String allPath = "";
        if (key == null) {
            keyPathMap.each {
                String newKey, Set<String> pathSet ->
                    pathSet.each {
                        String path ->
                            allPath += (path + ";\n")
                    }
            }
        } else {
            Set<String> pathSet = keyPathMap.get(key);
            if (pathSet == null) return "";
            pathSet.each {
                String path ->
                    allPath += (path + ";")
            }
        }
        return allPath;
    }

    private static boolean isMetaServices(String rootPath, File file) {
        if (file != null && file.exists()) {
            //文件存在
            String pathName = file.path.substring(rootPath.length())
            println("------isMetaServices pathName=${pathName}");
            if (pathName.contains("META-INF/services/serviceloader/")) {
                return true;
            }
        }
    }

    private boolean processLine(String line, Map<String, String> map) {
        if (extension.enableLog) {
            println("------serviceloader process line: ${line}")
        }
        //每一行必须是"key:className"
        String[] splits = line.split(":")
        if (splits != null && splits.length == 2) {
            String key = splits[0].trim();
            String value = splits[1].trim();
            if (key.length() > 0 && value.length() > 0) {
                if (map.containsKey(key)) {
                    throw new GradleException("You declared class:${value} in ${SERVICE_DIRECTION}")
                } else {
                    map.put(key, value);
                }
            }
        }
    }

    private void addService(String key, Map<String, String> map) {
        key = getName(key);
        if (key == null) return
        if (servicesMap.containsKey(key)) {
            Map<String, String> originMap = servicesMap.get(key);
            Set<String> keySet = map.keySet();
            for (String newkey : keySet) {
                if (originMap.containsKey(newkey) && !originMap.get(newkey).equals(map.get(newkey))) {
                    throw new GradleException("You declared class:${value} in ${SERVICE_DIRECTION}")
                }
            }
            originMap.putAll(map);
        } else {
            servicesMap.put(key, map)
        }
    }

    private void addUserService(String interfaceName, Set<String> set, String path) {
        if (interfaceName == null || interfaceName.empty) return
        if (!interfaceName.endsWith(USR_SUFFIX)) return
        interfaceName = interfaceName.substring(0, interfaceName.length() - "_use".length())
        if (useServiceMap.containsKey(interfaceName)) {
            Set<String> originaSet = useServiceMap.get(interfaceName)
            originaSet.addAll(set);
        } else {
            useServiceMap.put(interfaceName, set);
        }
        if (path == null || path.empty) return

        Map<String, Set<String>> keyPathMap = useFilePathMap.get(interfaceName)
        if (keyPathMap == null) {
            keyPathMap = new HashMap<>();
            useFilePathMap.put(interfaceName, keyPathMap);
        }
        set.each { String key ->
            Set<String> pathSet = keyPathMap.get(key)
            if (pathSet == null) {
                pathSet = new HashSet<>();
                keyPathMap.put(key, pathSet)
            }
            pathSet.add(path);
        }
    }

    private String getName(String name) {
        if (name == null) return
        if (name.endsWith(AUTO_SUFFIX)) {
            name = name.substring(0, name.length() - AUTO_SUFFIX.length())
        }
        return name;
    }

    private String processJar(File jarFile) {
        if (extension != null && extension.enableLog) {
            println("-----serviceLoader process jar file: ${jarFile.path}")
        }
        def tempJarFile = new File("jar.temp", jarFile.parentFile);
        if (tempJarFile.exists()) {
            tempJarFile.delete()
        }

        byte[] buf = new byte[1024 * 8]
        ZipFile zipFile = new ZipFile(jarFile);
        ZipInputStream zin = new ZipInputStream(new FileInputStream(jarFile));
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempJarFile));
        ZipEntry entry = zin.getNextEntry();
        while (entry != null) {
            String name = entry.getName();
            if (extension.enableLog) {
                println("-----serviceloader process jar entry:${name}")
            }
            if (name.contains("META-INF/services/serviceloader/"/*SERVICE_DIRECTION*/) && !entry.directory) {
                if (name.endsWith(USR_SUFFIX)) {
                    if (extension.enableLog) {
                        println("-----use serviceloader finds meta file in jar entry:${name}")
                    }
                    Set<String> set = new HashSet<>();
                    BufferedReader br = new BufferedReader(new InputStreamReader(zipFile.getInputStream()))
                    String line;
                    while ((line = br.readLine() != null)){
                        if(line != null && line.length() > 0){
                            set.add(line);
                        }
                    }
                    br.close()
                    if(!set.isEmpty()){
                        if (extension.enableLog) {
                            println("-----use serviceloader finds meta file in jar entry:${name}")
                        }
                        String[] splits = name.split("/")
                        addUserService(splits.last(),set,name)
                        entry = zin.getNextEntry()
                        continue
                    }
                }else{
                    if (extension.enableLog) {
                        println("-----use serviceloader finds meta file in jar entry:${name}")
                    }
                    Map<String,String> map = new HashMap<>();
                    BufferedReader br = new BufferedReader(new InputStreamReader(zipFile.getInputStream(entry)))
                    String line
                    while ((line = br.readLine()) != null){
                        processLine(line,map)
                    }
                    br.close();
                    if(!map.isEmpty()){
                        if (extension.enableLog) {
                            println("-----use serviceloader finds meta file in jar entry:${name}")
                        }
                        String[] splits = name.split("/")//splits.last() 返回数组最后一个元素
                        addService(splits.last(),map)
                        entry = zin.getNextEntry()
                        continue
                    }
                }
            }
            ZipEntry zipEntry =  new ZipEntry(name);
            if(ZipEntry.STORED == entry.getMethod()){
                zipEntry.setMethod(entry.getMethod());
                zipEntry.setSize(entry.getSize());
                zipEntry.setCompressedSize(entry.getCompressedSize());
                zipEntry.setCrc(entry.getCrc())
            }
            out.putNextEntry(zipEntry);

            int len;
            while ((len = zin.read(buf)) > 0){
                out.write(buf,0,len);
            }
            out.closeEntry()
            entry = zin.getNextEntry();
        }
        zin.close();
        out.close();
        tempJarFile.renameTo(jarFile);
    }

    private void addClassToJarFile(File classFile,File jarFile){
        if (extension.enableLog) {
            println("-----serviceLoader add class into jar file: ${jarFile.path}")
        }
        def tempJarFile = new File("temp.jar", jarFile.parentFile);
        if (tempJarFile.exists()) {
            tempJarFile.delete()
        }
        byte[] buf = new byte[1024 * 8]
        ZipFile zipFile = new ZipFile(jarFile);
        ZipInputStream zin = new ZipInputStream(new FileInputStream(jarFile));
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempJarFile));
        ZipEntry entry = zin.getNextEntry();
        while (entry != null) {
            String name = entry.getName();
            if (extension.enableLog) {
                println("-----serviceloader process jar entry:${name}")
            }

            ZipEntry zipEntry =  new ZipEntry(name);
            if(ZipEntry.STORED == entry.getMethod()){
                zipEntry.setMethod(entry.getMethod());
                zipEntry.setSize(entry.getSize());
                zipEntry.setCompressedSize(entry.getCompressedSize());
                zipEntry.setCrc(entry.getCrc())
            }
            out.putNextEntry(zipEntry);
            //
            int len;
            while ((len = zin.read(buf)) > 0){
                out.write(buf,0,len);
            }
            out.closeEntry()
            entry = zin.getNextEntry();
        }
        //TODO 路径确认
        ZipEntry zipEntry = new ZipEntry("")
        out.putNextEntry(zipEntry)
        FileInputStream classStream = new FileInputStream(classFile);
        int len;
        while ((len = classStream.read(buf)) > 0){
            out.write(buf,0,len)
        }
        out.closeEntry()
        classStream.close();
        zin.close();
        out.close();
    }

}

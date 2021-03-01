package com.example.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.api.BaseVariant
import com.android.builder.dependency.level2.AndroidDependency
import com.android.dex.ClassDef
import com.android.dex.Dex
import org.gradle.api.DomainObjectCollection
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

public class ServiceLoaderPlugin implements Plugin<Project> {
    //插件的名称
    private static final String PLUGIN_NAME = "service_loader"
    private ServiceLoaderThansform transform;
    public Map<String,Map<String,String>> servicesMap = new HashMap<>();

    @Override
    public void apply(Project project) {
        println("-------serviceloader start----")
        def isAppPlugin = project.plugins.withType(AppPlugin)
        if (!isAppPlugin) {
            return;
        }
        //创建extension
        ServiceLoaderExtension extension = project.extensions.create(PLUGIN_NAME, ServiceLoaderExtension)
        def isDebugTask = false;
        for (taskName in project.gradle.startParameter.taskNames) {
            System.out.println("-------taskName = " + taskName);
            /**
             * 1.因为这这里还没有开始执行task，所以拿不到task的成名，没法通过buildType判断
             * 2.所以直接分析当前的startParameter.taskNames
             * 3.task的名字不能缩写，必须是全称，这是一个小坑,例如assembleDebug，不能有任何形式的缩写
             */
            println("-------Serviceloader $taskName")
            if (taskName.endsWith("Debug")) {
                isDebugTask = true;
                break;
            }
        }
        if (!isDebugTask) {
            //创建transform
            transform = new ServiceLoaderThansform(project, extension);
            //def android = project.extensions.getByType(AppExtension)
            //注册transform
            project.android.registerTransform(transform);
        }

        project.afterEvaluate {
            /*if (!extension.enable) {
                return
            }*/
            applyTask(project, extension, project.android.applicationVariants);
        }

        println("-------serviceloader end----")
    }

    private void applyTask(Project project, ServiceLoaderExtension extension, DomainObjectCollection<BaseVariant> variants) {

        variants.all {
            BaseVariant variant ->
                def variantName = variant.name.capitalize()
                println("------variantName=$variantName")
                def mergeJavaResTask = project.tasks.findByName("transformResourcesWithMergeJavaResFor${variantName}")
                if (mergeJavaResTask == null) {
                    return;
                }

                def proGuardTask = null;
                proGuardTask = project.tasks.findByName("transformClassesAndResourcesWithProguardFor${variantName}")

                //compileDebugJavaWithJavac
                def javaWithJavacTask = project.tasks.findByName("compile${variantName}JavaWithJavac")
                FindServiceAction findServiceAction = new FindServiceAction()
                mergeJavaResTask.doLast {
                    if (mergeJavaResTask.getOutputs().getHasOutput()) {
                        findServiceAction.project = project;
                        findServiceAction.variant = variant;
                        findServiceAction.extension = extension;
                        findServiceAction.fileCollection = mergeJavaResTask.getOutputs().getFiles();
                        findServiceAction.execute(mergeJavaResTask);
                        if (transform != null) {
                            //把findServiceAction处理过的map存放起来，让transform在transform的时候通过javassient代码注入处理
                            servicesMap.putAll(findServiceAction.servicesMap)
                        }
                        if (proGuardTask == null) {
                            Set<File> filesForCheck = new HashSet<>();
                            if (javaWithJavacTask != null) {
                                FileCollection fileCollection = javaWithJavacTask.getOutputs().getFiles().getAsFileTree();
                                for (File file : fileCollection) {
                                    if (file.path.endsWith(".class")) {
                                        filesForCheck.add(file);
                                    }
                                }
                            }
                            //jar file
                            Iterator iterator = project.android.applicationVariants.iterator()
                            while (iterator.hasNext()) {
                                def applicationVariant = iterator.next();

                                if (mergeJavaResTask.variantName.equalsIgnoreCase((applicationVariant.
                                        flavorName + applicationVariant.buildType.name))) {
                                    if (applicationVariant.variantData.variantDependency.properties['compileClasspath'] != null) {
                                        //通过classLoader加载hook
                                        def consumedConfigTypeClazz = Class.forName("com.android.build.gradle.internal.publishing.AndroidArtifacts\$ConsumedConfigType");
                                        def artifactScopeClazz = Class.forName("com.android.build.gradle.internal.publishing.AndroidArtifacts\$ArtifactScope")
                                        def artifactTypeClazz = Class.forName("com.android.build.gradle.internal.publishing.AndroidArtifacts\$ArtifactType")
                                        def all = applicationVariant.variantData.scope.getArtifactCollection(consumedConfigTypeClazz.getField("RUNTIME_CLASSPATH").get(null),
                                                artifactScopeClazz.getField("ALL").get(null), artifactTypeClazz.getField("CLASSES").get(null)).getArtifacts();

                                        for (org.gradle.api.artifacts.result.ResolvedArtifactResult dependency : all) {
                                            filesForCheck.add(dependency.file);
                                        }
                                    } else {
                                        def all = applicationVariant.variantData.variantDependency.compileDependencies.allDependencies
                                        for (Dependency dependency : all) {
                                            if (dependency instanceof AndroidDependency) {
                                                AndroidDependency androidDependency = (AndroidDependency) dependency;
                                                filesForCheck.add(androidDependency.jarFile);
                                                List<File> libs = androidDependency.localJars
                                                if (libs != null) {
                                                    filesForCheck.add(libs);
                                                }
                                            } else {
                                                filesForCheck.add(dependency.artifactFile)
                                            }
                                        }
                                    }

                                }
                            }
                            if (extension.enableLog) {
                                for (File inputFile : filesForCheck) {
                                    println("------serviceloader input file:${inputFile.path}")
                                }
                            }
                            //checkServiceClass(project, variant, extension, filesForCheck, findServiceAction.classNameList);
                        }
                    }
                }
                if (extension.enableLog) {
                    println("---serviceload proGuardTask is exited:" + (proGuardTask != null ? "true" : "false"))
                }
                if (proGuardTask != null) {
                    proGuardTask.doFirst {
                        if (findServiceAction == null
                                || findServiceAction.classNameList == null
                                || findServiceAction.classNameList.size() == 0) {
                            return
                        }
                        println("------serviceloader proguardTask keeps services")
                        findServiceAction.classNameList.each { String className ->
                            proGuardTask.transform.keep("class $className")
                        }
                        FileCollection fileCollection = proGuardTask.getInputs().getFiles().getAsFileTree();
                        checkServiceClass(project, variant, extension, fileCollection.getFiles(), findServiceAction.classNameList)

                    }
                }
        }
    }

    private void checkServiceClass(Project project, BaseVariant variant, ServiceLoaderExtension exception, FileCollection files,
                                   List<String> classNameList) {
        if (variant.buildType.debuggable /*&& !exception.checkServiceOnDebug*/) {
            //debug 就不检查了!!!
            return;
        }
        long startTime = System.currentTimeMillis();
        FileCollection fileCollection = files.filter { File inputFile ->
            return inputFile.exists() && inputFile.path.endsWith(".dex")
        }
        for (File dexFile : fileCollection) {
            if (exception.enableLog) {
                println("----serviceloader check services in:${dexFile.path}")
            }
            Dex dex = new Dex(dexFile);
            for (ClassDef classDef : dex.classDefs()) {
                String name = dex.typeNames().get(classDef.getTypeIndex());
                name = getClassName(name);
                if (name != null && (name.length() != 0)) {
                    Iterator<String> iterator = classNameList.iterator();
                    while (iterator.hasNext()) {
                        if (name.endsWith(iterator.next())) {
                            iterator.remove();
                        }
                    }
                    if (classNameList.size() == 0) {
                        break
                    }
                }
                if (classNameList.size() == 0) {
                    break
                }
            }
        }
        println("-----serviceloade check class:${(System.currentTimeMillis() - startTime) / 1000}s")
        if (classNameList.size() != 0) {
            StringBuffer strBuffer = new StringBuffer();
            classNameList.each { String name ->
                strBuffer.append(name + ",")
            }
            throw new GradleException("You declared class{${strBuffer.toString()}}in ${FindServiceAction.SERVICE_DIRECTION},but not found")
        }
    }

    private String getClassName(String name) {
        if (name == null) {
            println("-----class name erroe !${name}")
            return null;
        } else if (!name.startsWith("L") || !name.endsWith(";")) {
            println("-----class name erroe !${name}")
            return null;
        } else {
            return name.subSequence(1, name.length() - 1).replace("/", ".")
        }
    }

    private void checkServiceClass(Project project, BaseVariant variant, ServiceLoaderExtension extension,
                                   Set<File> fileCollcetion, List<String> classNameList) {
        if (variant.buildType.debuggable /*&& !extension.checkServiceOnDebug*/) {
            return
        }
        long startTime = System.currentTimeMillis();
        for (File jarFile : fileCollcetion) {
            if (extension.enableLog) {
                println("-----serviceload checks service in:${jarFile.path}")
            }
            if (jarFile.path.endsWith(".jar")) {
                ZipFile zipFile = new ZipFile(jarFile);
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    String path = entries.nextElement().getName().replace("/", ".");
                    Iterator<String> iterator = classNameList.iterator();
                    while (iterator.hasNext()) {
                        if (path.endsWith(iterator.next() + ".class")) {
                            iterator.remove();
                        }
                    }
                    if (classNameList.size() == 0) {
                        break
                    }
                }
            } else if (jarFile.path.endsWith(".class")) {
                String path = jarFile.path.replace("/", ".");
                Iterator<String> iterator = classNameList.iterator();
                while (iterator.hasNext()) {
                    if (path.endsWith(iterator.next() + ".class")) {
                        iterator.remove();
                    }
                }
                if (classNameList.size() == 0) {
                    break
                }
            }

        }
        println("service loader check class ${(System.currentTimeMillis() - startTime) / 1000}s")
        if (classNameList.size() != 0) {
            StringBuilder strBuilder = new StringBuilder()
            classNameList.each { String name ->
                strBuilder.append(name + ",")
            }
            throw new GradleException("You declared class :${strBuilder.toString()} in ${FindServiceAction.SERVICE_DIRECTION},but not found")
        }

    }

}

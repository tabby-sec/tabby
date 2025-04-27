package tabby.core.sootup;

import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SootClass;
import sootup.core.model.SourceType;
import sootup.core.signatures.PackageName;
import sootup.core.types.ClassType;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaPackageName;
import sootup.java.core.types.JavaClassType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SootUpUtils {

    /**
     * 根据路径创建适当的输入位置
     *
     * @param path 文件或目录路径
     * @return AnalysisInputLocation实例
     */
    public static AnalysisInputLocation createInputLocation(Path path) {
        // 检查路径类型并创建适当的输入位置
        String pathStr = path.toString();

        if (pathStr.endsWith(".jar") || pathStr.endsWith(".war") || pathStr.endsWith(".zip")) {
            return new JavaClassPathAnalysisInputLocation(pathStr, SourceType.Application);
        } else {
            // 对于目录，使用DirectoryInputLocation
//            return new DirectoryInputLocation(path);
        }
        return null;
    }

    /**
     * 创建Java类型
     *
     * @param className 完全限定的类名
     * @return ClassType实例
     */
    public static ClassType createClassType(String className) {
        // 检查输入
        if (className == null || className.isEmpty()) {
            return null;
        }

        // 处理数组类型
        if (className.endsWith("[]")) {
            // 递归处理基础类型并创建数组类型
            String baseType = className.substring(0, className.length() - 2);
            return createClassType(baseType);
        }

        // 提取包名和类名
        int lastDotIndex = className.lastIndexOf('.');

        if (lastDotIndex == -1) {
            // 没有包名的情况 (默认包)
            return new JavaClassType(className,new PackageName(className));
        } else {
            String packageName = className.substring(0, lastDotIndex);
            String simpleClassName = className.substring(lastDotIndex + 1);
            return new JavaClassType(className, new JavaPackageName(packageName));
        }
    }

    /**
     * 将SootUp的SootClass转换为传统Soot的SootClass
     * 注意：这是一个桥接方法，用于过渡期间
     *
     * @param sootUpClass SootUp的SootClass
     * @return 传统Soot的SootClass
     */
    public static soot.SootClass convertToSootClass(SootClass sootUpClass) {
        // 这里仅作为示例，实际实现需要更复杂的转换逻辑
        soot.SootClass sootClass = new soot.SootClass(sootUpClass.getName());
        // TODO: 完善转换逻辑
        return sootClass;
    }

    /**
     * 从SootUp SootClass获取其所有父类和接口
     *
     * @param sootUpClass SootUp的SootClass
     * @param view        当前视图
     * @return 所有父类和接口的列表
     */
    public static List<String> getAllSuperClassesAndInterfaces(SootClass sootUpClass, sootup.core.views.View view) {
        List<String> result = new ArrayList<>();

        // 获取所有超类
        SootClass currentClass = sootUpClass;
        while (currentClass.getSuperclass().isPresent()) {
            String superClassName = currentClass.getSuperclass().get().getClassName();
            result.add(superClassName);

            // 获取超类的SootClass
            ClassType superClassType = createClassType(superClassName);
            currentClass = view.getClass(superClassType).orElse(null);
            if (currentClass == null) break;
        }

        // 获取所有接口
        currentClass = sootUpClass;
        for (ClassType interfaceName : currentClass.getInterfaces()) {
            result.add(interfaceName.getClassName());
            // 递归获取父接口 (如果需要)
        }

        return result;
    }
}
package tabby.core.sootup;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.frontend.ResolveException;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.typehierarchy.ViewTypeHierarchy;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootClass;
import sootup.java.core.views.JavaView;
import sootup.java.frontend.inputlocation.JavaSourcePathAnalysisInputLocation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class SootUpViewManager {
    private static final Logger log = LoggerFactory.getLogger(SootUpViewManager.class);
    private static SootUpViewManager instance;

    // 全局单例JavaView对象
    private JavaView globalView;

    /**
     * -- GETTER --
     * 检查全局视图是否已初始化
     *
     * @return 是否已初始化
     */
    // 是否已初始化
    @Getter
    private boolean initialized = false;

    private SootUpViewManager() {
        // 私有构造函数，防止外部实例化
    }

    /**
     * 获取SootUpViewManager的单例实例
     *
     * @return SootUpViewManager实例
     */
    public static synchronized SootUpViewManager getInstance() {
        if (instance == null) {
            instance = new SootUpViewManager();
        }
        return instance;
    }

    /**
     * 初始化全局JavaView
     *
     * @param classPaths 类路径列表
     */
    public synchronized void initializeView(List<String> classPaths) {
        if (classPaths == null || classPaths.isEmpty()) {
            throw new IllegalArgumentException("Classpaths cannot be empty");
        }

        // 如果已经初始化，记录日志但继续重新初始化
        if (initialized) {
            log.info("Re-initializing global JavaView");
        }

        try {
            // 转换classpath字符串为Path对象
            List<AnalysisInputLocation> inputLocations = new ArrayList<>();
            for (String path : classPaths) {
                Path classPath = Paths.get(path);
                if (path.endsWith(".java")) {
                    inputLocations.add(new JavaSourcePathAnalysisInputLocation(String.valueOf(classPath)));
                } else {
                    AnalysisInputLocation inputLocation = SootUpUtils.createInputLocation(classPath);
                    if (inputLocation != null) {
                        inputLocations.add(inputLocation);
                    }
                }
            }

            // 创建全局视图
            globalView = new JavaView(inputLocations);
            initialized = true;

            log.info("Initialized global JavaView with {} classpath entries", classPaths.size());

        } catch (Exception e) {
            log.error("Failed to initialize JavaView: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize JavaView", e);
        }
    }

    /**
     * 获取全局JavaView
     *
     * @return 全局JavaView，如果未初始化则返回null
     */
    public JavaView getView() {
        if (!initialized) {
            log.warn("Global JavaView not initialized. Call initializeView() first.");
        }
        return globalView;
    }

    /**
     * 根据类名获取SootClass
     *
     * @param classType 类型
     * @return SootClass实例，如果找不到则返回null
     */
    public JavaSootClass getClass(ClassType classType) {
        if (!initialized) {
            log.error("Global JavaView not initialized. Call initializeView() first.");
            return null;
        }

        try {
            return globalView.getClass(classType).orElse(null);
        } catch (ResolveException e) {
            log.debug("Could not resolve class {}: {}", classType.getClassName(), e.getMessage());
            return null;
        }
    }

    /**
     * 根据类名字符串获取SootClass
     *
     * @param className 类名
     * @return SootClass实例，如果找不到则返回null
     */
    public JavaSootClass getClass(String className) {
        if (!initialized) {
            log.error("Global JavaView not initialized. Call initializeView() first.");
            return null;
        }

        try {
            ClassType classType = SootUpUtils.createClassType(className);
            return globalView.getClass(classType).orElse(null);
        } catch (Exception e) {
            log.debug("Could not resolve class {}: {}", className, e.getMessage());
            return null;
        }
    }

    /**
     * 获取类型层次结构
     *
     * @return ViewTypeHierarchy实例
     */
    public ViewTypeHierarchy getTypeHierarchy() {
        if (!initialized) {
            log.error("Global JavaView not initialized. Call initializeView() first.");
            return null;
        }
        return (ViewTypeHierarchy) globalView.getTypeHierarchy();
    }

    /**
     * 重置全局视图
     */
    public synchronized void reset() {
        globalView = null;
        initialized = false;
        log.info("Global JavaView reset");
    }
}
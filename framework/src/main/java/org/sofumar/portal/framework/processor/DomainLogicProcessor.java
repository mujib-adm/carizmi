package org.sofumar.portal.framework.processor;

import org.sofumar.portal.framework.annotation.DomainLogicFor;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Annotation Processor to enforce 1-to-1 relationship between Value Objects and Business Logic classes.
 * It detects duplicate @DomainLogicFor annotations for the same VO class during compilation.
 */
@SupportedAnnotationTypes("org.sofumar.portal.framework.annotation.DomainLogicFor")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class DomainLogicProcessor extends AbstractProcessor {

    // Map to track VO -> BL mapping within the current compilation context
    private final Map<String, String> voToBlMap = new HashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Enforcing Domain Logic 1-to-1 mapping rules...");
        
        // Elements annotated in this round
        for (Element element : roundEnv.getElementsAnnotatedWith(DomainLogicFor.class)) {
            if (element instanceof TypeElement typeElement) {
                String blClassName = typeElement.getQualifiedName().toString();
                String voClassName = getVoClassNameFromAnnotation(typeElement);

                if (voClassName != null) {
                    if (voToBlMap.containsKey(voClassName)) {
                        String existingBl = voToBlMap.get(voClassName);
                        if (!existingBl.equals(blClassName)) {
                            processingEnv.getMessager().printMessage(
                                Diagnostic.Kind.ERROR,
                                String.format("Duplicate Business Logic detected for VO [%s]. Both [%s] and [%s] are annotated with @DomainLogicFor.",
                                    voClassName, existingBl, blClassName),
                                typeElement);
                        }
                    } else {
                        voToBlMap.put(voClassName, blClassName);
                    }
                }
            }
        }
        return true; 
    }

    /**
     * Extracts the VO class name from the @DomainLogicFor annotation.
     * We must handle Class<?> values as TypeMirror at compile time.
     */
    private String getVoClassNameFromAnnotation(TypeElement typeElement) {
        for (AnnotationMirror mirror : typeElement.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals(DomainLogicFor.class.getName())) {
                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                    if (entry.getKey().getSimpleName().toString().equals("value")) {
                        Object value = entry.getValue().getValue();
                        if (value instanceof TypeMirror typeMirror) {
                            return typeMirror.toString();
                        }
                    }
                }
            }
        }
        return null;
    }
}
package org.contractlib.jml;

import com.github.javaparser.ast.jml.type.JmlLogicType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import org.contractlib.ast.Type;

import java.util.Map;

/**
 * This class provides static methods to translate type expressions from Contract-LIB to JML.
 *
 * @author Wolfram Pfeifer
 */
public class Smt2JmlTypeTranslator {
    private static final Map<String, JmlLogicType.Primitive> CONTRACTLIB2JMLTYPES = Map.of(
        "Map", JmlLogicType.Primitive.MAP,
        "Set", JmlLogicType.Primitive.SET);

    private Smt2JmlTypeTranslator() {
    }

    public static com.github.javaparser.ast.type.Type translateType(Type type) {
        // TODO: at the moment, the JmlLogicTypes are swallowed by JavaParser and not printed!
        JmlLogicType.Primitive prim = CONTRACTLIB2JMLTYPES.get(type.toString());
        JmlLogicType lt = new JmlLogicType(prim);

        // TODO: workaround for the moment

        ClassOrInterfaceType ct = new ClassOrInterfaceType(null, type.toString());
        // TODO: only ClassOrInterfaceType at the moment, include e.g. primitive types ...
        return ct;
    }
}

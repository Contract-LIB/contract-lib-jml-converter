package org.contractlib.jml;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.jml.expr.JmlBinaryInfixExpr;
import org.contractlib.ast.Term;

import java.util.Map;

/**
 * This class provides static methods to translate terms from Contract-LIB to JML.
 *
 * @author Wolfram Pfeifer
 */
public class Smt2JmlTermTranslator {
    private static final Map<String, JMLFunction> FUNCTIONS_SMT2JML = Map.ofEntries(
        // core symbols (https://smt-lib.org/theories-Core.shtml)
        Map.entry("true", new JMLFunction("true", 0, true)),
        Map.entry("false", new JMLFunction("false", 0, true)),
        Map.entry("not", new JMLFunction("!", 1, true)),
        Map.entry("=>", new JMLFunction("==>", 2, true)),
        Map.entry("and", new JMLFunction("&&", 2, true)),
        Map.entry("or", new JMLFunction("||", 2, true)),
        //Map.entry("xor", new JMLFunction("false", 0, true)),
        Map.entry("=", new JMLFunction("==", 2, true)),
        //Map.entry("distinct", new JMLFunction("false", 0, true)),
        //Map.entry("ite", new JMLFunction("false", 0, true)),

        // theory of sequenes (https://cvc5.github.io/docs/cvc5-1.1.1/theories/sequences.html):
        Map.entry("seq.empty", new JMLFunction("\\seq_empty", 0, false)),
        Map.entry("seq.unit", new JMLFunction("\\seq_singleton", 1, false)),
        Map.entry("seq.len", new JMLFunction("\\seq_length", 1, false)),
        Map.entry("seq.nth", new JMLFunction("\\seq_get", 2, false)),
        Map.entry("seq.update", new JMLFunction("\\seq_upd", 3, false)),
        //"seq.extract
        Map.entry("seq.++", new JMLFunction("\\seq_concat", 2, false)),
        //"seq.at", ...
        Map.entry("seq.contains", new JMLFunction("\\seq_contains", 2, false))
        // ...
        );

    private record JMLFunction(String name, int args, boolean infix) {
    }

    private Smt2JmlTermTranslator() {
    }

    public static Expression translateTerm(Term term) {
        return switch (term) {
            case Term.Old o -> translateOld(o);
            case Term.Application a -> translateApp(a);
            case Term.Binder b -> translateBinder(b);
            case Term.Literal l -> translateLiteral(l);
            case Term.Variable v -> translateVar(v);
        };
    }

    private static Expression translateBinder(Term.Binder b) {
        // TODO
        return null;
    }

    private static Expression translateVar(Term.Variable v) {
        // TODO: check that variable is declared?
        return new NameExpr(v.name());
    }

    private static Expression translateLiteral(Term.Literal l) {
        // TODO: literal suffixes needed?
        return new NameExpr(l.value().toString());
    }

    private static Expression translateOld(Term.Old old) {
        return new MethodCallExpr("\\old", translateTerm(old.argument()));
    }

    private static Expression translateApp(Term.Application app) {
        Expression[] args = app.arguments().stream()
            .map(Smt2JmlTermTranslator::translateTerm).toArray(Expression[]::new);
        String smtFun = app.function();
        JMLFunction jmlFun = FUNCTIONS_SMT2JML.get(smtFun);
        String jmlFunName = smtFun; // default to name of smt function
        if (jmlFun != null) {
            jmlFunName = jmlFun.name();
        } else {
            System.err.println("Warning: Unclear how to translate SMT function " + smtFun
                + " to JML!");
        }
        if (args.length == 0) {
            // variable name
            // TODO: check that variable is declared?
            return new NameExpr(jmlFunName);
        } else if (args.length == 1) {
            return new MethodCallExpr(jmlFunName, args);
        } else if (args.length == 2) {
            // TODO: some functions in SMT are n-ary (and, or, ...). Not supported at the moment!
            if (jmlFun != null && jmlFun.infix()) {
                return new JmlBinaryInfixExpr(args[0], args[1], new SimpleName(jmlFunName));
            } else {
                return new MethodCallExpr(jmlFunName, args);
            }
        } else {
            throw new RuntimeException("N-ary functions with n >= 3 can not be translated at the moment.");
        }
    }
}

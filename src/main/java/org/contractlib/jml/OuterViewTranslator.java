package org.contractlib.jml;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.jml.body.JmlFieldDeclaration;
import com.github.javaparser.ast.jml.clauses.*;
import com.github.javaparser.ast.stmt.Behavior;
import org.contractlib.ast.*;
import org.contractlib.factory.Mode;
import org.contractlib.parser.ContractLibANTLRParser;
import org.contractlib.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class can be used to create the "outer view" of a data structure. This view consists of an
 * interface containing the abstractions as ghost fields, and declares all the methods with the
 * contracts as defined in Contract-LIB.
 *
 * @author Wolfram Pfeifer
 */
public class OuterViewTranslator {
    private final List<Command> commands;
    private final Map<String, CompilationUnit> classes = new HashMap<>();

    private OuterViewTranslator(List<Command> commands) {
        this.commands = commands;
    }

    static String translate(ContractLibANTLRParser<Term, Type, Abstraction, Datatype, FunDec, Command> converter) {

        OuterViewTranslator translator = new OuterViewTranslator(converter.getCommands());
        // translate the declared abstractions
        translator.convertAbstractionCommands();

        // translate the defined contracts (depends on the abstractions!)
        translator.commands.stream()
            .filter(c -> c instanceof Command.DefineContract)
            .forEach(c -> translator.convertContract((Command.DefineContract)c));

        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, CompilationUnit> cu : translator.classes.entrySet()) {
            result.append(cu.getValue().toString());
        }
        return result.toString();
    }

    private void convertContract(Command.DefineContract c) {
        String contractName = c.name();
        String[] splits = contractName.split("\\.");
        assert splits.length == 2;
        String className = splits[0];
        String methodName = splits[1];

        CompilationUnit cu = classes.get(className);
        assert cu != null;

        ClassOrInterfaceDeclaration cls = cu.getClassByName(className).get();
        MethodDeclaration method = cls.addMethod(methodName);

        int outParams = 0;
        for (Pair<String, Pair<Mode, Type>> p : c.formal()) {
            String paramName = p.first();
            Mode paramMode = p.second().first();
            Type paramType = p.second().second();

            if (paramMode == Mode.OUT || paramMode == Mode.INOUT) {
                outParams++;
            }

            if (paramMode == Mode.IN) {
                var pType = Smt2JmlTypeTranslator.translateType(paramType);
                var param = new Parameter(pType, paramName);
                method.addAndGetParameter(param);
            }
        }

        for (var contr : c.contracts()) {
            Term pre = contr.first();
            Term post = contr.second();

            JmlClause preClause = new JmlSimpleExprClause(JmlClauseKind.REQUIRES, null,
                null, Smt2JmlTermTranslator.translateTerm(pre));
            JmlClause postClause = new JmlSimpleExprClause(JmlClauseKind.ENSURES, null,
                null, Smt2JmlTermTranslator.translateTerm(post));
            NodeList<JmlClause> clauses = NodeList.nodeList(preClause, postClause);

            JmlContract jmlContract = new JmlContract(ContractType.METHOD, Behavior.NORMAL,
                null, NodeList.nodeList(), clauses, NodeList.nodeList());
            method.addContracts(jmlContract);
        }

        // at most two out parameters are allowed: this and return
        assert outParams <= 2;

    }

    private void convertAbstractionCommands() {
        commands.stream()
            .filter(c -> c instanceof Command.DeclareAbstractions)
            .forEach(c -> convertAbstractions((Command.DeclareAbstractions) c));
    }

    private void convertAbstractions(Command.DeclareAbstractions c) {
        Set<String> classNames = c.arities().stream().map(Pair::first).collect(Collectors.toSet());

        for (String cn : classNames) {
            CompilationUnit cu = new CompilationUnit();
            cu.addClass(cn);
            classes.put(cn, cu);
        }

        c.abstractions().stream().forEach(this::convertAbstraction);
    }

    private String convertAbstraction(Abstraction a) {

        StringBuilder result = new StringBuilder();
        for (var constr : a.constrs()) {
            // TODO: type params ignored for now
            String name = constr.first();

            List<Pair<String, List<Type>>> params = constr.second();

            CompilationUnit cu = classes.get(name);
            assert cu != null;

            for (var p : params) {
                String param = p.first();

                String[] splits = param.split("\\.");
                assert splits.length == 2;
                assert splits[0].equals(name);
                String paramName = splits[1];

                // TODO: why is this a list and not only a single type?
                List<Type> type = p.second();

                ClassOrInterfaceDeclaration cl = cu.getClassByName(name).get();

                Type t = type.get(0);
                com.github.javaparser.ast.type.Type fieldType = Smt2JmlTypeTranslator.translateType(t);

                FieldDeclaration fd = new FieldDeclaration(new NodeList<>(), fieldType, paramName);
                fd.addModifier(Modifier.DefaultKeyword.JML_GHOST);
                JmlFieldDeclaration decl = new JmlFieldDeclaration(NodeList.nodeList(), fd);
                cl.addMember(decl);
            }

            result.append(cu);
        }

        return result.toString();
    }
}

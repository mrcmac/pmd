/**
 * BSD-style license; for more info see http://pmd.sourceforge.net/license.html
 */

package net.sourceforge.pmd.lang.java.rule.performance;

import net.sourceforge.pmd.lang.ast.Node;
import net.sourceforge.pmd.lang.java.ast.ASTArgumentList;
import net.sourceforge.pmd.lang.java.ast.ASTAssignmentOperator;
import net.sourceforge.pmd.lang.java.ast.ASTConditionalExpression;
import net.sourceforge.pmd.lang.java.ast.ASTEqualityExpression;
import net.sourceforge.pmd.lang.java.ast.ASTLocalVariableDeclaration;
import net.sourceforge.pmd.lang.java.ast.ASTName;
import net.sourceforge.pmd.lang.java.ast.ASTPrimaryExpression;
import net.sourceforge.pmd.lang.java.ast.ASTStatementExpression;
import net.sourceforge.pmd.lang.java.ast.ASTVariableDeclaratorId;
import net.sourceforge.pmd.lang.java.rule.AbstractJavaRule;
import net.sourceforge.pmd.lang.java.typeresolution.TypeHelper;
import net.sourceforge.pmd.lang.symboltable.NameOccurrence;

public class UseStringBufferForStringAppendsRule extends AbstractJavaRule {
    
    @Override
    public Object visit(ASTVariableDeclaratorId node, Object data) {
        if (!TypeHelper.isA(node, String.class) || node.isArray()) {
            return data;
        }
        Node parent = node.jjtGetParent().jjtGetParent();
        if (!(parent instanceof ASTLocalVariableDeclaration)) {
            return data;
        }
        for (NameOccurrence no : node.getUsages()) {
            Node name = no.getLocation();
            ASTStatementExpression statement = name.getFirstParentOfType(ASTStatementExpression.class);
            if (statement == null) {
                continue;
            }
            ASTArgumentList argList = name.getFirstParentOfType(ASTArgumentList.class);
            if (argList != null && argList.getFirstParentOfType(ASTStatementExpression.class) == statement) {
                // used in method call
                continue;
            }
            ASTEqualityExpression equality = name.getFirstParentOfType(ASTEqualityExpression.class);
            if (equality != null && equality.getFirstParentOfType(ASTStatementExpression.class) == statement) {
                // used in condition
                continue;
            }
            ASTConditionalExpression conditional = name.getFirstParentOfType(ASTConditionalExpression.class);

            if (conditional != null) {
                Node thirdParent = name.jjtGetParent().jjtGetParent().jjtGetParent();
                if ((thirdParent == conditional || thirdParent.jjtGetParent() == conditional)
                        && conditional.getFirstParentOfType(ASTStatementExpression.class) == statement) {
                    // is used in ternary as only option (not appended to other
                    // string)
                    continue;
                }
            }
            if (statement.jjtGetNumChildren() > 0 && statement.jjtGetChild(0) instanceof ASTPrimaryExpression) {
                ASTName astName = statement.jjtGetChild(0).getFirstDescendantOfType(ASTName.class);
                if (astName != null) {
                    if (astName.equals(name)) {
                        ASTAssignmentOperator assignmentOperator = statement
                                .getFirstDescendantOfType(ASTAssignmentOperator.class);
                        if (assignmentOperator != null && assignmentOperator.isCompound()) {
                            addViolation(data, assignmentOperator);
                        }
                    } else if (astName.getImage().equals(name.getImage())) {
                        ASTAssignmentOperator assignmentOperator = statement
                                .getFirstDescendantOfType(ASTAssignmentOperator.class);
                        if (assignmentOperator != null && !assignmentOperator.isCompound()) {
                            addViolation(data, astName);
                        }
                    }
                }
            }
        }
        return data;
    }
}

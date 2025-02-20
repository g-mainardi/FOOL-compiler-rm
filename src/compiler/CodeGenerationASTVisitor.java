package compiler;

import compiler.AST.*;
import compiler.lib.*;
import compiler.exc.*;

import java.util.ArrayList;
import java.util.List;

import static compiler.lib.FOOLlib.*;
import static svm.ExecuteVM.MEMSIZE;

public class CodeGenerationASTVisitor extends BaseASTVisitor<String, VoidException> {

  CodeGenerationASTVisitor() {}
  CodeGenerationASTVisitor(boolean debug) {super(false,debug);} //enables print for debugging

	@Override
	public String visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		String declCode = null;
		for (Node dec : n.declist) declCode=nlJoin(declCode,visit(dec));
		return nlJoin(
			"push 0",	
			declCode, // generate code for declarations (allocation)			
			visit(n.exp),
			"halt",
			getCode()
		);
	}

	@Override
	public String visitNode(ProgNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp),
			"halt"
		);
	}

	@Override
	public String visitNode(FunNode n) {
		if (print) printNode(n,n.id);
		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.decList) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i = 0; i<n.parList.size(); i++) popParl = nlJoin(popParl,"pop");
		String funl = freshFunLabel();
		putCode(
			nlJoin(
				funl+":",
				"cfp", // set $fp to $sp value
				"lra", // load $ra value
				declCode, // generate code for local declarations (they use the new $fp!!!)
				visit(n.exp), // generate code for function body expression
				"stm", // set $tm to popped value (function result)
				popDecl, // remove local declarations from stack
				"sra", // set $ra to popped value
				"pop", // remove Access Link from stack
				popParl, // remove parameters from stack
				"sfp", // set $fp to popped value (Control Link)
				"ltm", // load $tm value (function result)
				"lra", // load $ra value
				"js"  // jump to to popped address
			)
		);
		return "push "+funl;		
	}

	@Override
	public String visitNode(VarNode n) {
		if (print) printNode(n,n.id);
		return visit(n.exp);
	}

	@Override
	public String visitNode(PrintNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.exp),
			"print"
		);
	}

	@Override
	public String visitNode(IfNode n) {
		if (print) printNode(n);
	 	String l1 = freshLabel();
	 	String l2 = freshLabel();		
		return nlJoin(
			visit(n.cond),
			"push 1",
			"beq "+l1,
			visit(n.el),
			"b "+l2,
			l1+":",
			visit(n.th),
			l2+":"
		);
	}

	@Override
	public String visitNode(EqualNode n) {
		if (print) printNode(n);
	 	String l1 = freshLabel();
	 	String l2 = freshLabel();
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"beq "+l1,
			"push 0",
			"b "+l2,
			l1+":",
			"push 1",
			l2+":"
		);
	}

	@Override
	public String visitNode(LessEqualNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"bleq "+l1,
				"push 0",
				"b "+l2,
				l1+":",
				"push 1",
				l2+":"
		);
	}

	public String visitNode(GreaterEqualNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.right),
				visit(n.left),
				"bleq "+l1,
				"push 0",
				"b "+l2,
				l1+":",
				"push 1",
				l2+":"
		);
	}

	@Override
	public String visitNode(NotNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.exp),
				"push 0",
				"beq "+l1,
				"push 0",
				"b "+l2,
				l1+":",
				"push 1",
				l2+":"
		);
	}

	@Override
	public String visitNode(AndNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left),
				"push 0",
				"beq "+l1,
				visit(n.right),
				"push 0",
				"beq "+l1,
				"push 1",
				"b "+l2,
				l1+":",
				"push 0",
				l2+":"
		);
	}

	public String visitNode(OrNode n) {
		if (print) printNode(n);
		String l1 = freshLabel();
		String l2 = freshLabel();
		return nlJoin(
				visit(n.left),
				"push 1",
				"beq "+l1,
				visit(n.right),
				"push 1",
				"beq "+l1,
				"push 0",
				"b "+l2,
				l1+":",
				"push 1",
				l2+":"
		);
	}

	@Override
	public String visitNode(TimesNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"mult"
		);	
	}

	public String visitNode(DivNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"div"
		);
	}

	@Override
	public String visitNode(PlusNode n) {
		if (print) printNode(n);
		return nlJoin(
			visit(n.left),
			visit(n.right),
			"add"				
		);
	}

	@Override
	public String visitNode(MinusNode n) {
		if (print) printNode(n);
		return nlJoin(
				visit(n.left),
				visit(n.right),
				"sub"
		);
	}

	@Override
	public String visitNode(CallNode n) {
		if (print) printNode(n,n.id);
		String argCode = null, getAR = null;
		for (int i=n.arglist.size()-1;i>=0;i--) argCode=nlJoin(argCode,visit(n.arglist.get(i)));
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
			"lfp", // load Control Link (pointer to frame of function "id" caller)
			argCode, // generate code for argument expressions in reversed order
			"lfp", getAR, // retrieve address of frame containing "id" declaration
                          // by following the static chain (of Access Links)
            "stm", // set $tm to popped value (with the aim of duplicating top of stack)
            "ltm", // load Access Link (pointer to frame of function "id" declaration)
            "ltm", // duplicate top of stack
            "push "+n.entry.offset, "add", // compute address of "id" declaration
			"lw", // load address of "id" function
            "js"  // jump to popped address (saving address of subsequent instruction in $ra)
		);
	}

	@Override
	public String visitNode(IdNode n) {
		if (print) printNode(n,n.id);
		String getAR = null;
		for (int i = 0;i<n.nl-n.entry.nl;i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
			"lfp", getAR, // retrieve address of frame containing "id" declaration
			              // by following the static chain (of Access Links)
			"push "+n.entry.offset, "add", // compute address of "id" declaration
			"lw" // load value of "id" variable
		);
	}

	@Override
	public String visitNode(BoolNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+(n.val?1:0);
	}

	@Override
	public String visitNode(IntNode n) {
		if (print) printNode(n,n.val.toString());
		return "push "+n.val;
	}

	// OBJECT-ORIENTED EXTENSION
	@Override
	public String visitNode(ClassNode n) {
		if (print) printNode(n,n.id);
		List<String> dispatchTable = new ArrayList<>();
		String dispatchTableCode = "";

		for (MethodNode method : n.methodList) {
			visit(method);
			String methodLabel = method.label;
			dispatchTable.add(method.offset, methodLabel);

			dispatchTableCode = nlJoin(
					dispatchTableCode,
					// Memorizzo l'indirizzo del metodo nell'Heap
					"push " + methodLabel,
					"lhp",
					"sw",
					// Incremento l'HP
					"lhp",
					"push 1",
					"add",
					"shp"
			);
		}

		return nlJoin(
				"lhp",
				dispatchTableCode
		);
	}

	@Override
	public String visitNode(MethodNode n) {
		if (print) printNode(n,n.id);
		String declCode = null, popDecl = null, popParl = null;
		for (Node dec : n.decList) {
			declCode = nlJoin(declCode,visit(dec));
			popDecl = nlJoin(popDecl,"pop");
		}
		for (int i = 0; i<n.parList.size(); i++) popParl = nlJoin(popParl,"pop");
		n.label = freshFunLabel();
		putCode(
				nlJoin(
						n.label+":",
						"cfp", // set $fp to $sp value
						"lra", // load $ra value
						declCode, // generate code for local declarations (they use the new $fp!!!)
						visit(n.exp), // generate code for function body expression
						"stm", // set $tm to popped value (function result)
						popDecl, // remove local declarations from stack
						"sra", // set $ra to popped value
						"pop", // remove Access Link from stack
						popParl, // remove parameters from stack
						"sfp", // set $fp to popped value (Control Link)
						"ltm", // load $tm value (function result)
						"lra", // load $ra value
						"js"  // jump to popped address
				)
		);
		return null;
	}

	@Override
	public String visitNode(EmptyNode n) {
		if (print) printNode(n);
		return nlJoin("push -1");
	}

	@Override
	public String visitNode(ClassCallNode n) {
		if (print) printNode(n, n.refId+"."+n.methodId);

		String argCode = null, getAR = null;
		for (int i=n.argList.size()-1;i>=0;i--) argCode=nlJoin(argCode,visit(n.argList.get(i)));
		for (int i = 0; i<n.nestingLevel-n.methodEntry.nl; i++) getAR=nlJoin(getAR,"lw");
		return nlJoin(
				"lfp", // load Control Link (pointer to frame of method "id1.id2()" caller)
				argCode,      // generate code for argument expressions in reversed order
				"lfp", getAR, // retrieve address of frame containing "id" declaration
				          	  // by following the static chain (of Access Links)
				"push "+n.classEntry.offset, "add", // compute address of "id" declaration
				"lw", // load value of "id" variable
				"stm", // set $tm to popped value (with the aim of duplicating top of stack)
				"ltm", // load Access Link (object pointer to frame of class "id" declaration)
				"ltm", // duplicate top of stack
				"lw",
				"push "+n.methodEntry.offset, "add", // compute address of "id" declaration
				"lw", // load address of "id" method
				"js"  // jump to popped address (saving address of subsequent instruction in $ra)
		);
	}

	@Override
	public String visitNode(NewNode n) {
		if (print) printNode(n,n.id);
		String argValueCode = null, argToHeap = null, getAR = null;
		for (int i=0;i<n.argList.size();i++)
			argValueCode=nlJoin(argValueCode,visit(n.argList.get(i)));
		for (int i=n.argList.size()-1;i>=0;i--) {
			argToHeap = nlJoin(
					argToHeap,
					// Metto nell'Heap
					"lhp",
					"sw",
					// Incremento l'HP
					"lhp",
					"push 1",
					"add",
					"shp"
			);
		}

		return nlJoin(
				argValueCode,
				argToHeap,
				// Prendo valore a MEMSIZE - offset
				"push "+(MEMSIZE+n.entry.offset),
				"lw",
				// Lo metto nell'Heap
				"lhp",
				"sw",
				// Pusho l'HP prima di incrementarlo
				"lhp",
				// Incremento l'HP
				"lhp",
				"push 1",
				"add",
				"shp"
		);
	}
}
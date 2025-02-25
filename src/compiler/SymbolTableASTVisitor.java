package compiler;

import java.util.*;

import compiler.AST.*;
import compiler.exc.*;
import compiler.lib.*;

public class SymbolTableASTVisitor extends BaseASTVisitor<Void,VoidException> {

	/**
	 * La symbol table è implementata come una lista di mappe,
	 * dove ogni mappa rappresenta un singolo scope (block) e mappa gli ID (keys)
	 * alle corrispondenti symbol table entries (values).
	 * <ul>
	 *   <li>Ogni mappa nella lista corrisponde a un nesting level.</li>
	 *   <li>Lo scope più interno (current nesting level) è all'ultima posizione della lista,
	 *       mentre lo scope più esterno è in prima posizione (globale).</li>
	 *   <li>Permette di gestire e accedere alle dichiarazioni di variabili e funzioni
	 *       entro gli scope appropriati durante il parsing e l'analisi.</li>
	 * </ul>
	 */
	private List<Map<String, STentry>> symTable = new ArrayList<>();

	/**
	 * Rappresenta la symbol table per le classi, dove:
	 * <ul>
	 *   <li>Le chiavi di primo livello sono stringhe che rappresentano i nomi delle classi.</li>
	 *
	 *   <li>Le mappe di secondo livello corrispondono alle symbol table di ciascuna classe.</li>
	 *
	 *   <li>Queste mappe di secondo livello associano identificatori (campi e metodi)
	 *       alle corrispondenti istanze di {@link STentry}.</li>
	 * </ul>
	 */
	private Map<String, Map<String, STentry>> classTable = new HashMap<>();
	private int nestingLevel=0; // current nesting level
	private int decOffset=-2; // counter for offset of local declarations at current nesting level 
	int stErrors=0;

	SymbolTableASTVisitor() {}
	SymbolTableASTVisitor(boolean debug) {super(debug);} // enables print for debugging

	/**
	 * Cerca la symbol table entry corrispondente all'ID passato, iterando attraverso
	 * la lista di Mappe, a partire dall'attuale livello di annidamento fino al
	 * livello più esterno (dal più alto, al più basso).
	 *
	 * @param id l'identificatore da cercare nella symbol table
	 * @return la voce della symbol table associata all'identificatore, o null
	 *         se l'identificatore non viene trovato in alcuna area di visibilità
	 */
	private STentry stLookup(String id) {
		int j = nestingLevel;
		STentry entry = null;
		while (j >= 0 && entry == null) 
			entry = symTable.get(j--).get(id);	
		return entry;
	}

	@Override
	public Void visitNode(ProgLetInNode n) {
		if (print) printNode(n);
		Map<String, STentry> hm = new HashMap<>();
		symTable.add(hm);
	    for (Node dec : n.declist) visit(dec);
		visit(n.exp);
		symTable.remove(0);
		return null;
	}

	@Override
	public Void visitNode(ProgNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}
	
	@Override
	public Void visitNode(FunNode n) {
		if (print) printNode(n);

		// Prendo la HashMap per il nesting level corrente
		Map<String, STentry> hm = symTable.get(nestingLevel);

		// Colleziono i tipi dei parametri
		List<TypeNode> parTypes = new ArrayList<>();
		for (ParNode par : n.parList) parTypes.add(par.getType());

		// Creo un STentry con: nesting level, Tipo e Offset
		STentry entry = new STentry(nestingLevel, new ArrowTypeNode(parTypes,n.retType),decOffset--);

		// Inserisco il mio ID + entry nella SymbolTable
		if (hm.put(n.id, entry) != null) {
			// Se è già presente -> errore
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}

		// Creo una nuova HashMap per lo scope interno e la aggiungo alla SymbolTable
		nestingLevel++;
		Map<String, STentry> hmn = new HashMap<>();
		symTable.add(hmn);

		// Salvo l'offset di questo livello prima di resettare per il prossimo
		int prevNLDecOffset=decOffset;
		decOffset=-2;

		// Imposto l'offset per i parametri (verso l'alto) e li metto nella nuova HashMap
		int parOffset=1;
		for (ParNode par : n.parList)
			if (hmn.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
				stErrors++;
			}

		// Visito le dichiarazioni della funzione (let)
		for (Node dec : n.decList) visit(dec);

		// Visito l'espressione della funzione (in)
		visit(n.exp);

		// Rimuovo l'HashMap perché esco dallo scope interno
		symTable.remove(nestingLevel--);

		// Ripristino l'offset precedente
		decOffset=prevNLDecOffset;
		return null;
	}

	@Override
	public Void visitNode(VarNode n) {
		if (print) printNode(n);

		// Eseguo l'espressione
		visit(n.exp);

		// Prendo la HashMap per il nesting level corrente
		Map<String, STentry> hm = symTable.get(nestingLevel);

		// Creo un STentry con: nesting level, Tipo e Offset
		STentry entry = new STentry(nestingLevel,n.getType(),decOffset--);

		// Inserisco il mio ID + entry nella SymbolTable
		if (hm.put(n.id, entry) != null) {
			System.out.println("Var id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}
		return null;
	}

	@Override
	public Void visitNode(PrintNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(IfNode n) {
		if (print) printNode(n);
		visit(n.cond);
		visit(n.th);
		visit(n.el);
		return null;
	}
	
	@Override
	public Void visitNode(EqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(LessEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	public Void visitNode(GreaterEqualNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(TimesNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	public Void visitNode(DivNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}
	
	@Override
	public Void visitNode(PlusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(MinusNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(NotNode n) {
		if (print) printNode(n);
		visit(n.exp);
		return null;
	}

	@Override
	public Void visitNode(AndNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	public Void visitNode(OrNode n) {
		if (print) printNode(n);
		visit(n.left);
		visit(n.right);
		return null;
	}

	@Override
	public Void visitNode(CallNode n) {
		if (print) printNode(n);

		// Cerco l'STentry della funzione dal nesting level più alto a quello più basso
		STentry entry = stLookup(n.id);
		if (entry == null) {
			System.out.println("Fun id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			// Se la trovo, la salvo nel nodo e imposto il nesting level a quello corrente
			n.entry = entry;
			n.nl = nestingLevel;
		}

		// Visito gli argomenti della funzione
		for (Node arg : n.arglist) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(IdNode n) {
		if (print) printNode(n);

		// Cerco l'STentry della variabile dal nesting level più alto a quello più basso
		STentry entry = stLookup(n.id);
		if (entry == null) {
			// Se non la trovo, errore
			System.out.println("Var or Par id " + n.id + " at line "+ n.getLine() + " not declared");
			stErrors++;
		} else {
			// Se la trovo, la salvo nel nodo e imposto il nesting level a quello corrente
			n.entry = entry;
			n.nl = nestingLevel;
		}
		return null;
	}

	@Override
	public Void visitNode(BoolNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	@Override
	public Void visitNode(IntNode n) {
		if (print) printNode(n, n.val.toString());
		return null;
	}

	// OBJECT-ORIENTED EXTENSION

	@Override
	public Void visitNode(ClassNode n) throws VoidException {
		if (print) printNode(n);

		// Prendo la HashMap per il nesting level corrente (globale)
		Map<String, STentry> globalSymTable = symTable.get(0);

		// Array vuoto per i tipi dei campi
		List<TypeNode> allFields = new ArrayList<>();

		// Array vuoto per i tipi dei metodi
		List<ArrowTypeNode> allMethods = new ArrayList<>();

		if (n.superId != null) {
			STentry superClassEntry = globalSymTable.get(n.superId);
			n.superEntry = superClassEntry;
			ClassTypeNode classType = (ClassTypeNode) superClassEntry.type;
            allFields.addAll(classType.allFields);
			allMethods.addAll(classType.allMethods);
		}
		// Creo un STentry con: nesting level, liste dei tipi (campi e metodi) e Offset
		STentry entry = new STentry(0, new ClassTypeNode(allFields, allMethods), decOffset--);
		n.setType(entry.type);

		// Inserisco il mio ID + entry nella SymbolTable
		if (globalSymTable.put(n.id, entry) != null) {
			System.out.println("Class id " + n.id + " at line "+ n.getLine() +" already declared");
			stErrors++;
		}

		nestingLevel++;
		Map<String, STentry> virtualTable = new HashMap<>();
		if (n.superId != null) {
			Map<String, STentry> superClassTable = classTable.get(n.superId);
			virtualTable.putAll(superClassTable);
		}
		symTable.add(virtualTable);
		classTable.put(n.id, virtualTable);

		int fieldOffset = -allFields.size() -1;
		Set<String> newFields = new HashSet<>();
		for (FieldNode field : n.fieldList) {
			if (print) printNode(field);
			STentry oldEntry = virtualTable.get(field.id);
			STentry fieldEntry = null;
            if (!newFields.add(field.id)) {
				System.out.println("Field id " +  field.id + " at line " + field.getLine() + " already declared");
				stErrors++;
            }
			if (oldEntry == null) {
				fieldEntry = new STentry(nestingLevel, field.getType(), fieldOffset--);
			} else {
				if (oldEntry.type instanceof ArrowTypeNode) {
					System.out.println("Cannot override method " + field.id + "() at line "
							+ field.getLine() + " with field " + field.id);
					stErrors++;
				}
				fieldEntry = new STentry(nestingLevel, field.getType(), oldEntry.offset);
			}
			virtualTable.put(field.id, fieldEntry);
			allFields.add(-fieldEntry.offset - 1, field.getType());
		}

		// Salvo l'offset di questo livello prima di resettare per il prossimo
		int prevNLDecOffset = decOffset;
		decOffset = allMethods.size();
		Set<String> newMethods = new HashSet<>();

		for (MethodNode method : n.methodList) {
			if (!newMethods.add(method.id)) {
				System.out.println("Method id " +  method.id + " at line " + method.getLine() + " already declared");
				stErrors++;
			}
			visit(method);
			allMethods.add(method.offset, (ArrowTypeNode) method.getType());
		}

		// Rimuovo l'HashMap perché esco dallo scope interno
		symTable.remove(nestingLevel--);

		// Ripristino l'offset precedente
		decOffset=prevNLDecOffset;

		return null;
	}

	@Override
	public Void visitNode(MethodNode n) throws VoidException {
		if (print) printNode(n);

		// Prendo la HashMap per il nesting level corrente
		Map<String, STentry> virtualTable = symTable.get(nestingLevel);

		// Colleziono i tipi dei parametri
		List<TypeNode> parTypes = new ArrayList<>();
		for (ParNode par : n.parList) parTypes.add(par.getType());

		STentry oldEntry = virtualTable.get(n.id);
		STentry methodEntry = null;

		if (oldEntry == null) {
			// Creo un STentry con: nesting level, Tipo e Offset
			methodEntry = new STentry(nestingLevel, new ArrowTypeNode(parTypes, n.retType), decOffset++);
		} else {
			if (!(oldEntry.type instanceof ArrowTypeNode)) {
				System.out.println("Cannot override field " + n.id + " at line "
						+ n.getLine() + " with method " + n.id +"()");
				stErrors++;
			}
			methodEntry = new STentry(nestingLevel, oldEntry.type, oldEntry.offset);
		}

		n.offset = methodEntry.offset;
		n.setType(methodEntry.type);

		// Inserisco l'ID del metodo + entry nella VirtualTable della classe o rimpiazzo se c'era già
		virtualTable.put(n.id, methodEntry);

		nestingLevel++;
		Map<String, STentry> methodScope = new HashMap<>();
		symTable.add(methodScope);

		// Salvo l'offset di questo livello prima di resettare per il prossimo
		int prevNLDecOffset=decOffset;
		decOffset=-2;

		// Imposto l'offset per i parametri (verso l'alto) e li metto nella nuova HashMap
		int parOffset=1;
		for (ParNode par : n.parList)
			if (methodScope.put(par.id, new STentry(nestingLevel,par.getType(),parOffset++)) != null) {
				System.out.println("Par id " + par.id + " at line "+ n.getLine() +" already declared");
				stErrors++;
			}

		// Visito le dichiarazioni della funzione (let)
		for (Node dec : n.decList) visit(dec);

		// Visito l'espressione della funzione (in)
		visit(n.exp);

		// Rimuovo l'HashMap perché esco dallo scope interno
		symTable.remove(nestingLevel--);

		// Ripristino l'offset precedente
		decOffset=prevNLDecOffset;
		return null;
	}

	@Override
	public Void visitNode(ClassCallNode n) {
		if (print) printNode(n);

		// Cerco l'STentry della reference dal nesting level più alto a quello più basso
		STentry entry = stLookup(n.refId);
		if (entry == null) {
			System.out.println("Reference id " + n.refId + " at line " + n.getLine() + " not declared");
			stErrors++;
		} else {
			n.classEntry = entry;
			n.nestingLevel = nestingLevel;
			// Prendo l'STentry del metodo dalla Class Table
			String classId = ((RefTypeNode) entry.type).id;
			STentry methodEntry = classTable.get(classId).get(n.methodId);
			if (methodEntry == null) {
				System.out.println("Method id " + n.refId + "." + n.methodId + " at line "+ n.getLine() + " not declared");
				stErrors++;
			} else {
				// Se lo trovo, la salvo nel nodo
				n.methodEntry = methodEntry;
			}
		}

		// Visito gli argomenti del metodo
		for (Node arg : n.argList) visit(arg);
		return null;
	}

	@Override
	public Void visitNode(NewNode n) {
		if (print) printNode(n);
		if (!classTable.containsKey(n.id)) {
			System.out.println("Class id " + n.id + " at line " + n.getLine() + " not declared");
			stErrors++;
		}
        n.entry = symTable.get(0).get(n.id);
		for (Node arg : n.argList) visit(arg);
		return null;
	}
	@Override
	public Void visitNode(EmptyNode n) {
		if (print) printNode(n);
		return null;
	}
}

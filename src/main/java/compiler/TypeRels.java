package compiler;

import compiler.AST.*;
import compiler.lib.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TypeRels {

	/**
	 * Maps a class name to its super-class name.
	 */
	public static Map<String,String> superType =  new HashMap<String,String>();

	public static boolean isSubtype(TypeNode a, TypeNode b) {
		return isSubClass(a, b)
				|| isSubFunc(a, b)
				|| (!areRefType(a, b) && a.getClass().equals(b.getClass()))
				|| ((a instanceof BoolTypeNode) && (b instanceof IntTypeNode))
				|| ((a instanceof EmptyTypeNode) && (b instanceof RefTypeNode));
	}

	private static boolean isSubFunc(TypeNode a, TypeNode b) {
		return (a instanceof ArrowTypeNode)
				&& (b instanceof ArrowTypeNode)
				&& isSubtype(((ArrowTypeNode) a).ret, ((ArrowTypeNode) b).ret)
				&& paramsCheck((((ArrowTypeNode) a).parlist), ((ArrowTypeNode) b).parlist);
	}

	private static boolean paramsCheck(List<TypeNode> params1, List<TypeNode> params2) {
		if  (params1.size() != params2.size())
			return false;
		for (int i = 0; i < params1.size(); i++) {
			if(!isSuperType(params1.get(i), params2.get(i)))
				return false;
		}
		return true;
	}

	private static boolean isSuperType(TypeNode a, TypeNode b) {
		return isSubtype(b, a);
	}

	private static boolean isSubClass(TypeNode a, TypeNode b) {
		return areRefType(a, b)
				&& recursiveSubClass(((RefTypeNode) a).id, ((RefTypeNode) b).id);
	}

	private static boolean recursiveSubClass(String id1, String id2) {
		if(id1.equals(id2))
			return true;
		if (!superType.containsKey(id1))
			return false;
		return recursiveSubClass(superType.get(id1), id2);
	}

	public static TypeNode lowestCommonAncestor(TypeNode a, TypeNode b) {
		if (isSubtype(a, b)) return b;
		if (isSubtype(b, a)) return a;

		// RefTypes: search Lowest Common
		if ((a instanceof RefTypeNode firstType) && (b instanceof RefTypeNode)) {
            while(superType.containsKey(firstType.id)) {
				firstType = new RefTypeNode(superType.get(firstType.id));
				if (isSubClass(b, firstType)) return firstType;
			}
		}

		// Every other case
		return null;
	}

	private static boolean areRefType(TypeNode a, TypeNode b) {
		return (a instanceof RefTypeNode) && (b instanceof RefTypeNode);
	}
}

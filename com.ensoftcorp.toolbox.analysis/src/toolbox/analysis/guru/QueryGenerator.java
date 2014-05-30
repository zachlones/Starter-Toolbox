package toolbox.analysis.guru;

import java.util.ArrayList;

import com.ensoftcorp.atlas.java.core.query.Attr.Node;
import com.ensoftcorp.atlas.java.core.query.Q;
import com.ensoftcorp.atlas.java.core.query.Attr.Edge;
import com.ensoftcorp.atlas.java.core.script.Common;



public class QueryGenerator {
	/**
	 * A method which returns the method
	 * @param context	A context from which to start searching
	 * @param packageName The package containing the class(es) and method
	 * @param className		A String containing the classes going from outermost class to innermost class delimited by $
	 * 						Assume anonymous inner classes start with a number and non-anonymous classes do not.
	 * @param returnType	Not fully qualified return type of the method
	 * @param parameters:	Not fully qualified parameters of the method
	 * @return
	 */
	public static Q findMethod(Q context, String packageName, String classNames,String methodName,String returnType, String[] parameters){
		//parse class names out of classNames
		String[] className = getStringArray(classNames);
		//Follow declare edges down to correct class.
		Q declareGraph = Common.universe().edgesTaggedWithAll(Edge.DECLARES);
		Q pkg = context.pkg(packageName);
		Q declaredClass = Common.stepTo(declareGraph, pkg).nodesTaggedWithAll(Node.CLASS).selectNode(Node.NAME, className[0]);

		//Follow declares down inner classes
		for(int i = 1; i<className.length;i++){
			if(className[i].contains("$")){
				Q declaredMethods = Common.stepTo(declareGraph,declaredClass).nodesTaggedWithAll(Node.METHOD);
				declaredClass = Common.stepTo(declareGraph,declaredMethods).nodesTaggedWithAll(Node.CLASS).selectNode(Node.NAME,className[i]);
			}
			else{
				declaredClass = Common.stepTo(declareGraph, declaredClass).nodesTaggedWithAll(Node.CLASS).selectNode(Node.NAME, className[i]);
			}
		}

		//Get methods with correct name from class
		Q methods = Common.stepTo(declareGraph, declaredClass).nodesTaggedWithAll(Node.METHOD).selectNode(Node.NAME,methodName);
		
		//Remove methods with wrong return type
		Q returnGraph = Common.universe().edgesTaggedWithAll(Edge.RETURNS).retainEdges();
		Q returnTypeNode = returnGraph.selectNode(Node.NAME, returnType);
		Q methodsReturningCorrect = returnGraph.reverseStep(returnTypeNode);
		methods = methods.intersection(methodsReturningCorrect);
		
		//remove methods with wrong number of params
		Q paramEdges = context.edgesTaggedWithAny(Edge.PARAM).retainEdges();
		if (parameters.length!=0){
		//remove methods with too many params
		Q extraParamNodes = paramEdges.selectNode(Node.PARAMETER_INDEX, parameters.length);
		Q methodsWithTooManyParam = paramEdges.reverseStep(extraParamNodes).nodesTaggedWithAny(Node.METHOD);
		methods = methods.difference(methodsWithTooManyParam);
		//remove methods with too few params
		Q paramNodes = paramEdges.selectNode(Node.PARAMETER_INDEX, parameters.length-1);
		Q methodsWithRightAmountOfParams = paramEdges.reverseStep(paramNodes).nodesTaggedWithAny(Node.METHOD);
		methods = methods.intersection(methodsWithRightAmountOfParams);
		} else{
			Q methodsWithParams = paramEdges.retainEdges().nodesTaggedWithAny(Node.METHOD);
			methods = methods.difference(methodsWithParams);
		}
		//remove methods with wrong param types
		Q typeOfEdges = Common.universe().edgesTaggedWithAny(Edge.TYPEOF).retainEdges();
		for(int j = 0; j<parameters.length; j++){
			Q paramType = Common.universe().selectNode(Node.NAME, parameters[j]);
			Q paramTypeParams = typeOfEdges.reverseStep(paramType).selectNode(Node.PARAMETER_INDEX, j);
			Q methodsWithParam = paramEdges.reverseStep(paramTypeParams).nodesTaggedWithAny(Node.METHOD);
			methods = methods.intersection(methodsWithParam);
		}
		
	return methods;
	}
	
	/**
	 * A method to return the method given the universe as a context
	 * @param packageName The package containing the class(es) and method
	 * @param className		A String containing the classes going from outermost class to innermost class delimited by $
	 * 						Assume anonymous inner classes start with a number and non-anonymous classes do not.
	 * @param returnType	Not fully qualified return type of the method
	 * @param parameters:	Not fully qualified parameters of the method
	 * @return
	 */
	public static Q findMethod( String packageName, String className,String methodName,String returnType, String[] parameters){
		return findMethod(Common.universe(),packageName, className,methodName, returnType, parameters);
	}
	
	public static Q test(){
		String[] classString = new String[] {"String"};
		String[] parameter = new String[] {"Locale"};
		String[] parameter2 = new String[] {};
		return findMethod("","MyMain$1$1$innerHiddenClass","getQ","int",parameter2);
	}
	
	/**
	 * A helper method for parsing the the order of classes and inner classes from a string
	 * @param classNames order of classes going from outermost to innermost delimited by $
	 * @return A string array of each class's name going from outermost to innermost
	 */
	public static String[] getStringArray(String classNames){
		ArrayList<String> list = new ArrayList<String>();
		int count = 0;
		int lastDollar = -1;
		boolean condition = true;
		while(condition){
			int locationOfDollar = classNames.indexOf("$", lastDollar+1);
			String clas;
			if(locationOfDollar>0){
				clas = classNames.substring(lastDollar+1,locationOfDollar);
			}
			else{
				clas = classNames.substring(lastDollar+1);
				condition = false;
			}
			if(Character.isDigit(clas.charAt(0))){
				clas=list.get(count-1) + "$"+ clas;
			}
			list.add(count++, clas);
			lastDollar = locationOfDollar;
		}
		String[] className =list.toArray(new String[list.size()]);
		return className;
	}
}

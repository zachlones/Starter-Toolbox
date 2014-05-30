package toolbox.analysis.guru;

import toolbox.analysis.Script;

import com.ensoftcorp.atlas.java.core.query.Attr.Edge;
import com.ensoftcorp.atlas.java.core.query.Attr.Node;
import com.ensoftcorp.atlas.java.core.query.Q;
import com.ensoftcorp.atlas.java.core.script.Common;

public class MainMethodFinder extends Script {

	@Override
	public String getDescription() {
		return "Returns public static void main methods that take a single String array called from the given context.";
	}

	@Override
	public String[] getAssumptions() {
		String[] assumptions = {"Main methods are methods.", // done
								"Main methods are public.", // done
								"Main methods are static.", // done
								"Main methods return void.", // done
								"Main methods take a single String[]",
								"Main methods may be final.", // done
								"Main methods may have the strictfp keyword.", // done
								"Main methods may be synchronized."}; // done
		return assumptions;
	}

	@Override
	protected Q evaluateEnvelope() {
		// find public static methods named "main"
		Q publicStaticMethods = context.nodesTaggedWithAll(Node.METHOD, Node.IS_PUBLIC, Node.IS_STATIC);
		Q mainMethods = publicStaticMethods.selectNode(Node.NAME, "main");
		
		// remove methods that are not void
		Q masterReturnNodes = context.nodesTaggedWithAny(Node.IS_MASTER_RETURN);
		Q declaresEdges = context.edgesTaggedWithAny(Edge.DECLARES);
		Q nonVoidMethods = declaresEdges.reverse(masterReturnNodes).nodesTaggedWithAny(Node.METHOD);
		mainMethods = mainMethods.difference(nonVoidMethods);
		
		// remove methods without parameters
		Q paramEdges = context.edgesTaggedWithAny(Edge.PARAM);
		Q methodsWithParams = paramEdges.retainEdges().nodesTaggedWithAny(Node.METHOD);
		mainMethods = mainMethods.intersection(methodsWithParams);
		
		// remove methods with more than 1 parameter
		Q secondParamNodes = paramEdges.retainEdges().selectNode(Node.PARAMETER_INDEX, 1);
		Q mainMethodsWithMoreThan1Param = paramEdges.reverseStep(secondParamNodes).nodesTaggedWithAny(Node.METHOD);
		mainMethods = mainMethods.difference(mainMethodsWithMoreThan1Param);
		
		// removes methods with the parameters that are not String[]
		Q typeOfEdges = Common.universe().edgesTaggedWithAny(Edge.TYPEOF).retainEdges();
		Q oneDimensionalStringArray = Common.universe().selectNode(Node.NAME, "String[]");
		Q stringArrayParams = typeOfEdges.reverseStep(oneDimensionalStringArray);
		Q methodsWithStringArrayParams = paramEdges.reverseStep(stringArrayParams).nodesTaggedWithAny(Node.METHOD);
		mainMethods = mainMethods.intersection(methodsWithStringArrayParams);
			
		return mainMethods;
	}

}

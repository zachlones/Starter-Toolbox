package toolbox.analysis.guru;

import com.ensoftcorp.atlas.java.core.db.graph.Graph;
import com.ensoftcorp.atlas.java.core.db.graph.GraphElement;
import com.ensoftcorp.atlas.java.core.db.graph.GraphElement.NodeDirection;
import com.ensoftcorp.atlas.java.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.java.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.java.core.query.Q;
import com.ensoftcorp.atlas.java.core.script.Common;

public class Degrees {

	public static void calculateDegrees(Q context){
		Graph graph = context.eval();
		AtlasSet<GraphElement> nodes = graph.nodes();
		for(GraphElement node : nodes){
			AtlasSet<GraphElement> inEdges = graph.edges(node, NodeDirection.IN);
			long numInEdges = inEdges.size();
			node.attr().put("inDegree", numInEdges);
			
			AtlasSet<GraphElement> outEdges = graph.edges(node, NodeDirection.OUT);
			long numOutEdges = outEdges.size();
			node.attr().put("outDegree", numOutEdges);
			node.tags().add("degreed");
		}
	}
	
	/**
	 * Returns a set of the highest value degree nodes for the given context and
	 * edge direction.
	 * @param context
	 * @param direction
	 * @return
	 */
	public static Q getHighestDegreeNodes(Q context, NodeDirection direction){
		Graph graph = context.eval();
		AtlasSet<GraphElement> nodes = graph.nodes().taggedWithAll("degreed");
		AtlasSet<GraphElement> highestNodes = new AtlasHashSet<GraphElement>();
		String dir;
		if(direction == NodeDirection.IN){
			dir = "inDegree";
		} else if (direction == NodeDirection.OUT){
			dir = "outDegree";
		}
		else{
			return Common.toQ(Common.toGraph(highestNodes));
		}
		long highest = 0;
		for(GraphElement node : nodes){
			if(node.attr().get(dir) != null){
				long degree = (Long) node.attr().get(dir);
				if(highestNodes.isEmpty()){
					highestNodes.add(node);
					highest = degree;
				} else {
					if(degree > highest){
						highestNodes.clear();
						highestNodes.add(node);
						highest = degree;
					} else if(degree == highest){
						highestNodes.add(node);
					}
				}
			}
		} 


		Graph graphResult = Common.toGraph(highestNodes);
		Q qResult = Common.toQ(graphResult);
		return qResult;
	}
	
}

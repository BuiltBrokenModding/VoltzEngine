package resonant.lib.type;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A node inside of a tree structure.
 *
 * @param <S> - Self type
 */
public class TreeNode<S extends TreeNode>
{
	public S parent;
	public Set<S> children;

	public S addChild(S perm)
	{
		children.add(perm);
		perm.parent = this;
		return perm;
	}

	/**
	 * Checks recursively to see if any of the children permission can match the given permission.
	 *
	 * @param targetPerm - The target match to find.
	 * @return True if the tree structure contains the targetPerm.
	 */
	public boolean exists(S targetPerm)
	{
		if (this.equals(targetPerm))
		{
			return true;
		}

		for (S perm : children)
		{
			if (perm.exists(targetPerm))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * @return Gets the hiearchy of the tree ordered from the root node to the current node.
	 */
	public List<S> getHierarchy()
	{
		List<S> hiearchy = new ArrayList();

		S currentParent = parent;

		while (currentParent != null)
		{
			hiearchy.add(currentParent);
			currentParent = (S) currentParent.parent;
		}

		return hiearchy;
	}

}
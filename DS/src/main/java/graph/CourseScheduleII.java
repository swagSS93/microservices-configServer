package graph;

//  same as Course Schedule

/*
There are a total of numCourses courses you have to take, labeled from 0 to numCourses - 1. You are given an array prerequisites where prerequisites[i] = [ai, bi] indicates that you must take course bi first if you want to take course ai.

        For example, the pair [0, 1], indicates that to take course 0 you have to first take course 1.
        Return true if you can finish all courses. Otherwise, return false.



        Example 1:

        Input: numCourses = 2, prerequisites = [[1,0]]
        Output: true
        Explanation: There are a total of 2 courses to take.
        To take course 1 you should have finished course 0. So it is possible.
        Example 2:

        Input: numCourses = 2, prerequisites = [[1,0],[0,1]]
        Output: false
        Explanation: There are a total of 2 courses to take.
        To take course 1 you should have finished course 0, and to take course 0 you should also have finished course 1. So it is impossible.
*/

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class CourseSchedule {
    public static void main(String args[]){
        int V = 4 ;
        int edges[][] = {{0, 0}, {1, 2}, {2, 0}, {2,3} };

        System.out.println(canFinish(V, edges));
    }

    public static boolean canFinish(int numCourses, int[][] prerequisites) {
        ArrayList<Integer> ans = new ArrayList<>();

        ArrayList<ArrayList<Integer>> adj =  new ArrayList<>();
        int[] inDegree = new int[numCourses];

        for(int i=0; i < numCourses; i++)
            adj.add(new ArrayList<>());

        for(int i = 0 ; i< prerequisites.length; i++){
            int u = prerequisites[i][0];
            int v = prerequisites[i][1];
            adj.get(v).add(u);
            inDegree[u]++;
        }

        Queue<Integer> queue = new LinkedList<>();

        for(int i = 0 ; i < numCourses; i++){
            if(inDegree[i] == 0)
                queue.add(i);
        }

        while (queue.size() > 0) {
            int vertex = queue.remove();
            ans.add(vertex);
            for(int ele : adj.get(vertex)){
                if(inDegree[ele] != 0)
                    inDegree[ele]--;

                if(inDegree[ele] == 0){
                    queue.add(ele);
                }
            }
        }
        return (numCourses == ans.size());

    }
}

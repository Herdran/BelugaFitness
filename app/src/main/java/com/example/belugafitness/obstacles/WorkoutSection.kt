package com.example.belugafitness.obstacles

class WorkoutSection {
    private var obstaclesFullList: ArrayList<Obstacle> = ArrayList()
    var obstaclesList: ArrayList<Obstacle> = ArrayList()


    private fun defineObstacles() {
        obstaclesFullList.add(RectangleFromTopObstacle(0.5f))
        obstaclesFullList.add(RectangleFromTopObstacle(0.4f))
        obstaclesFullList.add(RectangleFromTopObstacle(0.2f))

        obstaclesFullList.add(RectangleFromLeftObstacle(0.4f))
        obstaclesFullList.add(RectangleFromLeftObstacle(0.2f))

        obstaclesFullList.add(RectangleFromRightObstacle(0.2f))
        obstaclesFullList.add(RectangleFromRightObstacle(0.4f))

        obstaclesFullList.add(HoldCircleWithHandObstacle(0.6f, 0.2f, 0.2f))
        obstaclesFullList.add(HoldCircleWithHandObstacle(0.2f, 0.2f, 0.2f))
        obstaclesFullList.add(HoldCircleWithHandObstacle(0.4f, 0.2f, 0.2f))
        obstaclesFullList.add(HoldCircleWithHandObstacle(0.3f, 0.5f, 0.2f))

        obstaclesFullList.add(HoldCirclesWithBothHandsObstacle(0.7f, 0.1f,
            0.3f, 0.1f,0.2f))
//        two hands down
        obstaclesFullList.add(HoldCirclesWithBothHandsObstacle(0.7f, 0.6f,
            0.3f, 0.6f,0.2f))
//        two up
        obstaclesFullList.add(HoldCirclesWithBothHandsObstacle(0.7f, 0.1f,
            0.3f, 0.6f,0.2f))
//        one up
        obstaclesFullList.add(HoldCirclesWithBothHandsObstacle(0.7f, 0.6f,
            0.3f, 0.1f,0.2f))
//        other up



        obstaclesFullList.add(HoldCirclesWithBothHandsObstacle(0.7f, 0.2f,
            0.3f, 0.5f,0.2f))
//        weird
        obstaclesFullList.add(HoldCirclesWithBothHandsObstacle(0.2f, 0.2f,
            0.3f, 0.5f,0.2f))
//

    }


    fun generateWorkout() {
        defineObstacles()
        obstaclesList = getRandomElements(obstaclesFullList, 10)
    }


    fun <T> getRandomElements(inputList: ArrayList<T>, size: Int): ArrayList<T> {
        val outputList = ArrayList<T>()
        val indices = inputList.indices.shuffled().take(size)

        for (index in indices) {
            outputList.add(inputList[index])
        }

        return outputList
    }
}
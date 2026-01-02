package io.jorgen.posebaby

import android.graphics.PointF

object PoseLibrary {
    val ReferencePoses: Map<String, BodySkeleton> = mapOf(
        "standing_confident" to BodySkeleton(
            // Hands on hips
            leftShoulder = PointF(0.35f, 0.20f),
            rightShoulder = PointF(0.65f, 0.20f),
            leftElbow = PointF(0.25f, 0.35f),
            rightElbow = PointF(0.75f, 0.35f),
            leftWrist = PointF(0.40f, 0.50f),
            rightWrist = PointF(0.60f, 0.50f),
            leftHip = PointF(0.40f, 0.50f),
            rightHip = PointF(0.60f, 0.50f),
            leftKnee = PointF(0.40f, 0.75f),
            rightKnee = PointF(0.60f, 0.75f),
            leftAnkle = PointF(0.40f, 0.95f),
            rightAnkle = PointF(0.60f, 0.95f)
        ),
        "sitting_casual" to BodySkeleton(
            // Sitting with one leg crossed
            leftShoulder = PointF(0.35f, 0.30f),
            rightShoulder = PointF(0.65f, 0.30f),
            leftElbow = PointF(0.30f, 0.45f),
            rightElbow = PointF(0.70f, 0.45f),
            leftWrist = PointF(0.42f, 0.58f),
            rightWrist = PointF(0.58f, 0.58f),
            leftHip = PointF(0.40f, 0.60f),
            rightHip = PointF(0.60f, 0.60f),
            leftKnee = PointF(0.40f, 0.80f),
            rightKnee = PointF(0.65f, 0.80f),
            leftAnkle = PointF(0.40f, 0.95f),
            rightAnkle = PointF(0.45f, 0.78f) // Crossed over
        ),
        "leaning" to BodySkeleton(
            // Leaning against wall (left side of frame)
            leftShoulder = PointF(0.30f, 0.25f),
            rightShoulder = PointF(0.60f, 0.30f),
            leftElbow = PointF(0.25f, 0.40f),
            rightElbow = PointF(0.65f, 0.45f),
            leftWrist = PointF(0.30f, 0.55f),
            rightWrist = PointF(0.60f, 0.55f),
            leftHip = PointF(0.35f, 0.55f),
            rightHip = PointF(0.60f, 0.55f),
            leftKnee = PointF(0.40f, 0.75f),
            rightKnee = PointF(0.65f, 0.75f),
            leftAnkle = PointF(0.45f, 0.95f),
            rightAnkle = PointF(0.60f, 0.95f)
        ),
         "leaning_wall" to BodySkeleton(
             // Alias for leaning to match prompt example just in case
            leftShoulder = PointF(0.30f, 0.25f),
            rightShoulder = PointF(0.60f, 0.30f),
            leftElbow = PointF(0.25f, 0.40f),
            rightElbow = PointF(0.65f, 0.45f),
            leftWrist = PointF(0.30f, 0.55f),
            rightWrist = PointF(0.60f, 0.55f),
            leftHip = PointF(0.35f, 0.55f),
            rightHip = PointF(0.60f, 0.55f),
            leftKnee = PointF(0.40f, 0.75f),
            rightKnee = PointF(0.65f, 0.75f),
            leftAnkle = PointF(0.45f, 0.95f),
            rightAnkle = PointF(0.60f, 0.95f)
        )
    )

    fun getScaledPose(poseId: String, width: Int, height: Int): BodySkeleton? {
        // Try exact match, or fallback to fuzzy key match if needed, or default
        val normalized = ReferencePoses[poseId] ?: ReferencePoses.values.firstOrNull() ?: return null

        fun scale(point: PointF?): PointF? {
            return if (point != null) {
                PointF(point.x * width, point.y * height)
            } else {
                null
            }
        }

        return BodySkeleton(
            leftShoulder = scale(normalized.leftShoulder),
            rightShoulder = scale(normalized.rightShoulder),
            leftElbow = scale(normalized.leftElbow),
            rightElbow = scale(normalized.rightElbow),
            leftWrist = scale(normalized.leftWrist),
            rightWrist = scale(normalized.rightWrist),
            leftHip = scale(normalized.leftHip),
            rightHip = scale(normalized.rightHip),
            leftKnee = scale(normalized.leftKnee),
            rightKnee = scale(normalized.rightKnee),
            leftAnkle = scale(normalized.leftAnkle),
            rightAnkle = scale(normalized.rightAnkle)
        )
    }
}

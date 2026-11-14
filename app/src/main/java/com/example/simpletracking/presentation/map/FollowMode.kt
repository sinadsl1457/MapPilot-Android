package com.example.simpletracking.presentation.map

/**
 * 카메라가 내 위치를 따라가는 방식. 내 위치 버튼을 누를 때마다 순서대로 바뀐다.
 * 단, 지도를 옮겨 둔 상태(NONE + 카메라가 내 위치 아님)에서 누르면 카메라만 옮기고 모드는 그대로다.
 */
enum class FollowMode {
    /** 따라가지 않는다. 지도를 자유롭게 움직일 수 있다. */
    NONE,

    /** 카메라가 내 위치를 따라간다. 지도는 북쪽이 위. */
    FOLLOW,

    /** 따라가면서 지도도 돌린다. 이동 중에는 진행 방향, 정지 중에는 폰이 향한 방향이 위. */
    FOLLOW_WITH_HEADING;

    /** 버튼을 눌렀을 때 다음 모드: NONE → FOLLOW → FOLLOW_WITH_HEADING → NONE */
    val next: FollowMode
        get() = when (this) {
            NONE -> FOLLOW
            FOLLOW -> FOLLOW_WITH_HEADING
            FOLLOW_WITH_HEADING -> NONE
        }
}

/** 사용자가 손으로 지도를 움직인 방식. 따라가기를 풀 때 쓴다. */
enum class MapGesture { PAN, ROTATE }

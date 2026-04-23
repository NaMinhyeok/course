-- 참고용 DDL (실제 실행은 Hibernate create-drop 으로 처리)
-- 로컬/개발 H2 MySQL 모드 기준

CREATE TABLE course (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '강의 ID',
    creator_id     BIGINT        NOT NULL COMMENT '강의 생성자 ID',
    title          VARCHAR(100)  NOT NULL COMMENT '강의 제목',
    description    VARCHAR(2000) NOT NULL COMMENT '강의 설명',
    price          DECIMAL(19,0) NOT NULL COMMENT '강의 가격',
    capacity       INT           NOT NULL COMMENT '최대 수강 인원',
    start_at       DATETIME(6)   NOT NULL COMMENT '수강 시작 일시',
    end_at         DATETIME(6)   NOT NULL COMMENT '수강 종료 일시',
    course_status  VARCHAR(20)   NOT NULL COMMENT '강의 상태: DRAFT / OPEN / CLOSED',
    status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT '엔티티 상태: ACTIVE / DELETED',
    created_at     DATETIME(6)   NOT NULL COMMENT '생성 시각',
    updated_at     DATETIME(6)   NOT NULL COMMENT '수정 시각',
    PRIMARY KEY (id),
    INDEX idx_course_creator_id (creator_id),
    INDEX idx_course_course_status (course_status)
) COMMENT = '강의 기본 정보';

CREATE TABLE course_seats (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '좌석 관리 ID',
    course_id       BIGINT        NOT NULL COMMENT '강의 ID',
    capacity        INT           NOT NULL COMMENT '강의 정원',
    reserved_count  INT           NOT NULL COMMENT '현재 예약된 좌석 수',
    version         BIGINT        NOT NULL COMMENT '낙관 락 버전',
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT '엔티티 상태: ACTIVE / DELETED',
    created_at      DATETIME(6)   NOT NULL COMMENT '생성 시각',
    updated_at      DATETIME(6)   NOT NULL COMMENT '수정 시각',
    PRIMARY KEY (id),
    UNIQUE INDEX udx_course_seats_course_id (course_id)
) COMMENT = '강의별 좌석 및 정원 관리';


CREATE TABLE enrollment (
    id                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '수강 신청 ID',
    course_id          BIGINT        NOT NULL COMMENT '신청한 강의 ID',
    user_id            BIGINT        NOT NULL COMMENT '수강생 ID',
    enrollment_status  VARCHAR(20)   NOT NULL COMMENT '수강 신청 상태: PENDING / CONFIRMED / CANCELLED',
    confirmed_at       DATETIME(6)   NULL COMMENT '결제 확정 시각. CANCELLED 상태가 되어도 유지',
    status             VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE' COMMENT '엔티티 상태: ACTIVE / DELETED',
    created_at         DATETIME(6)   NOT NULL COMMENT '생성 시각',
    updated_at         DATETIME(6)   NOT NULL COMMENT '수정 시각',
    PRIMARY KEY (id),
    INDEX idx_enrollment_user_id (user_id)
) COMMENT = '사용자별 수강 신청 이력';

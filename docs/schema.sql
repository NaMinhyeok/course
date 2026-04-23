-- 참고용 DDL (실제 실행은 Hibernate create-drop 으로 처리)
-- 로컬/개발 H2 MySQL 모드 기준

CREATE TABLE course (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    creator_id     BIGINT        NOT NULL,
    title          VARCHAR(100)  NOT NULL,
    description    VARCHAR(2000) NOT NULL,
    price          DECIMAL(19,0) NOT NULL,
    capacity       INT           NOT NULL,
    start_at       DATETIME(6)   NOT NULL,
    end_at         DATETIME(6)   NOT NULL,
    course_status  VARCHAR(20)   NOT NULL,                   -- DRAFT / OPEN / CLOSED
    status         VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',  -- BaseEntity: ACTIVE / DELETED
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_course_creator_id (creator_id),
    INDEX idx_course_course_status (course_status)
);

CREATE TABLE course_seats (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    course_id       BIGINT        NOT NULL,
    capacity        INT           NOT NULL,
    reserved_count  INT           NOT NULL,
    version         BIGINT        NOT NULL,
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',  -- BaseEntity: ACTIVE / DELETED
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE INDEX udx_course_seats_course_id (course_id)
);

CREATE TABLE enrollment (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    course_id          BIGINT        NOT NULL,
    user_id            BIGINT        NOT NULL,
    enrollment_status  VARCHAR(20)   NOT NULL,                 -- PENDING / CONFIRMED / CANCELLED
    status             VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',-- BaseEntity: ACTIVE / DELETED
    created_at         DATETIME(6)   NOT NULL,
    updated_at         DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_enrollment_user_id (user_id)
);

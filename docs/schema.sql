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

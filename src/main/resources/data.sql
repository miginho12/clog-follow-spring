-- 로컬 실행용 시드 데이터 (alice/carol 공개, bob 비공개)
insert into users (nickname, is_public) values ('alice', true);
insert into users (nickname, is_public) values ('bob', false);
insert into users (nickname, is_public) values ('carol', true);

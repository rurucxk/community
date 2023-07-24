/*页面加载完成之后调用*/
$(function (){
    $("#topBtn").click(setTop);
    $("#wonderfulBtn").click(setWonderful);
    $("#deleteBtn").click(setDelete);
});

function like(btn,entityType,entityId,entityUserId,postId){
    $.post(
        CONTEXT_PATH + "/like",
        {"entityType":entityType,"entityId":entityId,"entityUserId":entityUserId,"postId":postId},
        function (data){
            data = $.parseJSON(data);
            if(data.code === 0){
                $(btn).children("i").text(data.likeCount);
                $(btn).children("b").text(data.likeStatus === 1 ? '已赞' : '赞');
            }else {
                alert(data.msg);
            }
        }
    );
}
/*置顶*/
function setTop(){
    if($("#topBtn").hasClass("btn-info")) {
        /*置顶*/
        $.post(
            CONTEXT_PATH + "/discuss/top",
            {"id": $("#postId").val(), "type": 1},
            function (data) {
                data = $.parseJSON(data);
                if (data.code === 0) {
                    location.reload();
                } else {
                    alert(data.msg);
                }
            }
        )
    }else {
        /*取消置顶*/
        $.post(
            CONTEXT_PATH + "/discuss/top",
            {"id": $("#postId").val(), "type": 0},
            function (data) {
                data = $.parseJSON(data);
                if (data.code === 0) {
                    location.reload();
                } else {
                    alert(data.msg);
                }
            }
        )
    }
}
/*加精*/
function setWonderful(){
    if($("#wonderfulBtn").hasClass("btn-info")) {
        $.post(
            CONTEXT_PATH + "/discuss/wonderful",
            {"id": $("#postId").val(), "status": 1},
            function (data) {
                data = $.parseJSON(data);
                if (data.code === 0) {
                    location.reload();
                } else {
                    alert(data.msg);
                }
            }
        )
    }else {
        /*取消加精*/
        $.post(
            CONTEXT_PATH + "/discuss/wonderful",
            {"id": $("#postId").val(), "status": 0},
            function (data) {
                data = $.parseJSON(data);
                if (data.code === 0) {
                    location.reload();
                } else {
                    alert(data.msg);
                }
            }
        )
    }
}
/*删除*/
function setDelete(){
    $.post(
        CONTEXT_PATH + "/discuss/delete",
        {"id":$("#postId").val()},
        function (data){
            data = $.parseJSON(data);
            if(data.code === 0){
                /*删除完直接跳转到主页*/
                location.href = CONTEXT_PATH + "/index";
            }else {
                alert(data.msg);
            }
        }
    )
}
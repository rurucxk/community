$(function(){
	$("form").submit(check_data);
	$("input").focus(clear_error);
});

function check_data() {
	var pwd0 = $("#old-password").val();
	var pwd1 = $("#new-password").val();
	var pwd2 = $("#confirm-password").val();
	if(pwd1 != pwd2) {
		$("#confirm-password").addClass("is-invalid");
		return false;
	}else if(pwd0 == pwd1){
		$("#confirm-password").addClass("is-invalid");
		$(".invalid-feedback").text("两次输入的密码不能相同"); // 修改无效反馈元素的文本内容
		return false;
	}else{
		return true;
	}

}

function clear_error() {
	$(this).removeClass("is-invalid");
}
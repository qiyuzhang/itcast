angular.module('AppRouterService', [ 'ui.router' ]).config(

function($stateProvider, $urlRouterProvider) {

	$urlRouterProvider.otherwise('/login');
	$stateProvider.state("login", {
		url : "/login",
		templateUrl : './views/login.html', // 页面
		controller : 'loginCtrl' // 控制器
	}).state('home', {
		url : '/home',
		templateUrl : './views/home.html',
		controller : 'homeCtrl'

	})
	// 产品管理
	.state("home.productlist", {
		url : "/productlist",
		templateUrl : './views/product_manage/product.html',
		controller : 'productList'
	})

	.state("home.productAdd", {
		url : "/productAdd",
		templateUrl : "./views/product_manage/product_add.html",
		controller : "productAdd"
	})

	.state("home.productEdit", {
		url : "/productEdit/:proId",
		templateUrl : "./views/product_manage/product_edit.html",
		controller : "productEdit"
	})

	// 债权查询
	.state("home.rightsCheck", {
		url : "/rightsCheck",
		templateUrl : './views/rights/check.html',
		controller : 'rightcheckCtrl'
	})
	// 债券录入
	.state("home.entering", {
		url : "/entering/:dId",
		templateUrl : './views/rights/entering.html',
		controller : 'entering'
	})

	// 债券还款计划查询
	.state("home.repayment", {
		url : "/repayment",
		templateUrl : './views/rights/repayment.html',
		controller : 'repaymentCtrl'
	})

	// 债券结算失败列表
	.state("home.fail", {
		url : "/fail",
		templateUrl : './views/rights/fail.html',
		controller : 'failCtrl'
	})

	// 批量导入
	.state("home.multiple", {
		url : "/multiple",
		templateUrl : './views/rights/multiple.html',
		controller : 'multiple'
	})
	// 待匹配资金队列
	.state("home.matching_fund_queue", {
		url : "/matching_fund_queue",
		templateUrl : './views/match_manage/matching_fund_queue.html',
		controller : 'matchfundqueueCtrl'
	})
	// 待匹配债权队列
	.state("home.toMatch", {
		url : "/toMatch",
		templateUrl : './views/together/toMatchRights.html',
		controller : 'toMatch'
	})

}).config(function($sceProvider) {
	$sceProvider.enabled(false);
}).run(
		function($rootScope, $state, AuthService) {
			$rootScope.$on('$stateChangeStart', function(event, toState) {
				if (toState.access != null && toState.access.isAuth
						&& !AuthService.isAuthenticated()
						&& !AuthService.getToken()) {
					event.preventDefault();
					$state.go('login')
				}
			});
			$rootScope.LoginAdmin = '';
			if (AuthService.isAuthenticated() && AuthService.getToken()) {
				$rootScope.LoginAdmin = AuthService.getCookie('adminuserinfo');
			}
		});

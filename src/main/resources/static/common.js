// 通用AJAX设置，自动添加Authorization头
$(document).ready(function() {
    // 设置AJAX全局默认值
    $.ajaxSetup({
        beforeSend: function(xhr) {
            // 从localStorage获取用户信息
            const userInfo = JSON.parse(localStorage.getItem('userInfo'));
            if (userInfo && userInfo.token) {
                // 添加Authorization头
                xhr.setRequestHeader('Authorization', 'Bearer ' + userInfo.token);
            }
        },
        error: function(xhr, status, error) {
            // 如果返回401未授权，跳转到登录页面
            if (xhr.status === 401) {
                window.location.href = '/login.html';
            }
        }
    });
    
    // 统一的退出登录函数
    window.logout = function() {
        // 先调用退出登录API
        $.ajax({
            url: '/api/logout',
            type: 'POST',
            success: function(response) {
                // 退出成功，清除localStorage并跳转到登录页面
                localStorage.removeItem('userInfo');
                window.location.href = '/login.html';
            },
            error: function(xhr, status, error) {
                // 退出失败，仍然清除localStorage并跳转到登录页面
                localStorage.removeItem('userInfo');
                window.location.href = '/login.html';
            }
        });
    };
    
    // 从localStorage获取用户信息并更新页面显示
    try {
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null');
        if (userInfo && userInfo.username) {
            // 更新用户名称
            $('.user-name').text(userInfo.username);
            // 更新欢迎信息中的用户名
            $('#welcomeUsername').text(userInfo.username);
        }
    } catch (error) {
        console.error('获取用户信息失败:', error);
    }
    
    // 深色模式切换
    function initThemeToggle() {
        const themeToggle = $('#themeToggle');
        const themeIcon = $('#themeIcon');
        const html = $('html');
        
        // 检查元素是否存在
        if (!themeToggle.length || !themeIcon.length) {
            return; // 如果没有主题切换元素，直接返回
        }
        
        // 检查用户设备偏好
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
        const savedTheme = localStorage.getItem('theme') || (prefersDark ? 'dark' : 'light');
        
        // 应用主题
        function applyTheme(theme) {
            html.attr('data-theme', theme);
            localStorage.setItem('theme', theme);
            if (theme === 'dark') {
                themeIcon.removeClass('fa-moon-o').addClass('fa-sun-o');
            } else {
                themeIcon.removeClass('fa-sun-o').addClass('fa-moon-o');
            }
        }
        
        applyTheme(savedTheme);
        
        // 切换主题
        themeToggle.click(function() {
            const currentTheme = html.attr('data-theme');
            const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
            applyTheme(newTheme);
        });
    }
    
    // 初始化主题切换
    initThemeToggle();
    
    // 更新页面上的系统名
    function updateSystemName() {
        $.ajax({
            url: '/api/admin/system/status',
            type: 'GET',
            dataType: 'json',
            success: function(response) {
                if (response.code === 0) {
                    var systemName = response.data.systemName || 'XX物流管理平台';
                    
                    // 更新页面标题
                    var pageTitle = document.title;
                    if (pageTitle.includes('XX物流管理平台')) {
                        document.title = pageTitle.replace('XX物流管理平台', systemName);
                    }
                    
                    // 更新页面中所有包含"XX物流管理平台"的元素
                    $('body').find('*').each(function() {
                        if ($(this).children().length === 0 && $(this).text().includes('XX物流管理平台')) {
                            $(this).text($(this).text().replace('XX物流管理平台', systemName));
                        }
                    });
                }
            },
            error: function() {
                // 请求失败，不处理
            }
        });
    }
    
    // 调用更新系统名函数
    updateSystemName();
});
// Runs before the page is drawn, so the right theme and language are there from the first frame.
(function () {
	var root = document.documentElement;

	function read(key) {
		try {
			return localStorage.getItem(key);
		} catch (e) {
			return null;
		}
	}

	var theme = read("avie29.theme");
	if (theme !== "dark" && theme !== "light") {
		theme = "system";
	}
	var systemDark = window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
	root.setAttribute("data-theme-setting", theme);
	root.setAttribute("data-theme", theme === "system" ? (systemDark ? "dark" : "light") : theme);

	var lang = read("avie29.lang");
	if (lang !== "de" && lang !== "en") {
		lang = (navigator.language || "en").toLowerCase().indexOf("de") === 0 ? "de" : "en";
	}
	root.setAttribute("lang", lang);
})();

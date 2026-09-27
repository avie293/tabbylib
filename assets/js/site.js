// Layout (top bar, mod list, footer), theme and language for every page.
(function () {
	"use strict";

	// ---------------------------------------------------------------- configuration

	var SITE = {
		name: "Avie29",
		icon: "/assets/img/avie29.svg",
		github: "https://github.com/avie293",
		modrinth: "https://modrinth.com/user/Avie"
	};

	// slug: the project on Modrinth (modrinth.com/mod/<slug>). null = not published yet, the page then shows a note.
	// Set TabbyLib's slug here once it is published.
	// type: "mod" or "datapack" - decides the section in the side list and which downloads are shown.
	// tagline: one short line per language, shown on the project's card on the start page.
	var TYPES = [
		{ id: "mod", title: "mods.title" },
		{ id: "datapack", title: "datapacks.title" }
	];

	var MODS = [
		{
			id: "tabbylib",
			name: "TabbyLib",
			type: "mod",
			slug: null,
			icon: "/assets/img/icons/tabbylib.png",
			page: "/tabbylib/",
			source: null,
			tagline: {
				de: "Config-Library für Minecraft-Mods",
				en: "Config library for Minecraft mods"
			}
		},
		{
			id: "day-counter",
			name: "Day Counter",
			type: "mod",
			slug: "avies-day-counter",
			icon: "/assets/img/icons/day-counter.png",
			page: "/day-counter/",
			source: "https://github.com/avie293/day-counter",
			tagline: {
				de: "Zeigt den aktuellen Minecraft-Tag im HUD",
				en: "Shows the current Minecraft day in your HUD"
			}
		},
		{
			id: "ping-display",
			name: "Ping Display",
			type: "mod",
			slug: "avies-ping-display",
			icon: "/assets/img/icons/ping-display.png",
			page: "/ping-display/",
			source: "https://github.com/avie293/Avie-s-Ping-Display",
			tagline: {
				de: "Ping als farbiger Text in der Tabliste",
				en: "Ping as colored text in the tab list"
			}
		},
		{
			id: "day-counter-datapack",
			name: "Day Counter",
			type: "datapack",
			slug: "aviesdaycounter-datapack",
			icon: "/assets/img/icons/day-counter.png",
			page: "/day-counter-datapack/",
			source: null,
			tagline: {
				de: "Der aktuelle Minecraft-Tag in der Actionbar – ganz ohne Mods",
				en: "The current Minecraft day in the action bar – no mods needed"
			}
		}
	];

	// ---------------------------------------------------------------- translations

	var TEXT = {
		de: {
			"theme.label": "Design",
			"theme.dark": "Dunkel",
			"theme.light": "Hell",
			"theme.system": "System",
			"lang.label": "Sprache",
			"lang.de": "Deutsch",
			"lang.en": "Englisch",
			"mods.title": "Mods",
			"datapacks.title": "Datapacks",
			"mods.loading": "lädt",
			"latest.title": "Neueste Versionen",
			"latest.open": "Zur Mod-Seite",
			"latest.open.datapack": "Zur Datapack-Seite",
			"latest.published": "Veröffentlicht am {date}",
			"latest.downloads": "{count} Downloads",
			"downloads.title": "Downloads",
			"downloads.download": "Herunterladen",
			"downloads.none": "Noch keine Version für {loader}.",
			"downloads.all": "Alle Versionen auf Modrinth",
			"modrinth.loading": "Lade Versionen von Modrinth",
			"modrinth.error": "Modrinth ist gerade nicht erreichbar. Versuch es später noch einmal oder öffne die Seite direkt auf Modrinth.",
			"modrinth.missing": "Noch nicht auf Modrinth veröffentlicht.",
			"link.modrinth": "Modrinth",
			"link.source": "Quellcode",
			"link.wiki": "Wiki",
			"link.maven": "Maven",
			"footer.disclaimer": "KEIN OFFIZIELLES MINECRAFT-PRODUKT. NICHT VON MOJANG ODER MICROSOFT GENEHMIGT ODER MIT MOJANG ODER MICROSOFT VERBUNDEN.",
			"footer.trademark": "Minecraft ist eine Marke von Mojang Synergies AB.",
			"footer.privacy": "Datenschutzerklärung",
			"footer.font": "Schrift: Monocraft (SIL Open Font License)",
			"type.beta": "Beta",
			"type.alpha": "Alpha"
		},
		en: {
			"theme.label": "Theme",
			"theme.dark": "Dark",
			"theme.light": "Light",
			"theme.system": "System",
			"lang.label": "Language",
			"lang.de": "German",
			"lang.en": "English",
			"mods.title": "Mods",
			"datapacks.title": "Datapacks",
			"mods.loading": "loading",
			"latest.title": "Latest releases",
			"latest.open": "Open mod page",
			"latest.open.datapack": "Open datapack page",
			"latest.published": "Released on {date}",
			"latest.downloads": "{count} downloads",
			"downloads.title": "Downloads",
			"downloads.download": "Download",
			"downloads.none": "No version for {loader} yet.",
			"downloads.all": "All versions on Modrinth",
			"modrinth.loading": "Loading versions from Modrinth",
			"modrinth.error": "Modrinth can not be reached right now. Try again later or open the project on Modrinth directly.",
			"modrinth.missing": "Not published on Modrinth yet.",
			"link.modrinth": "Modrinth",
			"link.source": "Source code",
			"link.wiki": "Wiki",
			"link.maven": "Maven",
			"footer.disclaimer": "NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.",
			"footer.trademark": "Minecraft is a trademark of Mojang Synergies AB.",
			"footer.privacy": "Privacy Policy",
			"footer.font": "Font: Monocraft (SIL Open Font License)",
			"type.beta": "Beta",
			"type.alpha": "Alpha"
		}
	};

	function lang() {
		return document.documentElement.getAttribute("lang") === "de" ? "de" : "en";
	}

	/** Translated text; {name} placeholders are replaced from values. */
	function t(key, values) {
		var text = (TEXT[lang()] && TEXT[lang()][key]) || TEXT.en[key] || key;
		if (values) {
			Object.keys(values).forEach(function (name) {
				text = text.split("{" + name + "}").join(values[name]);
			});
		}
		return text;
	}

	function applyTranslations(root) {
		(root || document).querySelectorAll("[data-i18n]").forEach(function (element) {
			element.textContent = t(element.getAttribute("data-i18n"));
		});
		var body = document.body;
		var title = body.getAttribute("data-title-" + lang()) || body.getAttribute("data-title-en");
		if (title) {
			document.title = title;
		}
	}

	// ---------------------------------------------------------------- preferences

	function store(key, value) {
		try {
			localStorage.setItem(key, value);
		} catch (e) {
			// Private mode or blocked storage: the choice just is not remembered
		}
	}

	var systemDark = window.matchMedia ? window.matchMedia("(prefers-color-scheme: dark)") : null;

	function applyTheme(setting) {
		var root = document.documentElement;
		root.setAttribute("data-theme-setting", setting);
		var dark = setting === "dark" || (setting === "system" && systemDark && systemDark.matches);
		root.setAttribute("data-theme", dark ? "dark" : "light");
		updateDropdowns();
	}

	function setTheme(setting) {
		store("avie29.theme", setting);
		applyTheme(setting);
	}

	function setLang(value) {
		store("avie29.lang", value);
		applyLang(value);
	}

	function applyLang(value) {
		document.documentElement.setAttribute("lang", value);
		updateDropdowns();
		applyTranslations();
		document.dispatchEvent(new CustomEvent("avie29:lang", { detail: value }));
	}

	if (systemDark) {
		var onSystemChange = function () {
			if (document.documentElement.getAttribute("data-theme-setting") === "system") {
				applyTheme("system");
			}
		};
		if (systemDark.addEventListener) {
			systemDark.addEventListener("change", onSystemChange);
		} else if (systemDark.addListener) {
			systemDark.addListener(onSystemChange);
		}
	}

	// ---------------------------------------------------------------- layout

	function element(tag, attributes, children) {
		var node = document.createElement(tag);
		Object.keys(attributes || {}).forEach(function (name) {
			if (name === "text") {
				node.textContent = attributes[name];
			} else {
				node.setAttribute(name, attributes[name]);
			}
		});
		(children || []).forEach(function (child) {
			if (child) {
				node.appendChild(child);
			}
		});
		return node;
	}

	// ---------------------------------------------------------------- dropdowns

	var SVG = {
		system: '<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="3" y="4" width="18" height="12" rx="2"/><path d="M8 20h8M12 16v4"/></svg>',
		dark: '<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg>',
		light: '<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg>',
		chevron: '<svg class="chevron" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M2.5 4.5 6 8l3.5-3.5"/></svg>',
		check: '<svg class="check" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 8.5l3.2 3L13 4.5"/></svg>'
	};

	function svg(name) {
		var holder = document.createElement("span");
		holder.innerHTML = SVG[name];
		return holder.firstChild;
	}

	function flag(src) {
		return element("img", { "class": "flag", src: src, alt: "", width: "22", height: "14" });
	}

	var dropdowns = [];
	var dropdownCount = 0;

	/**
	 * A small accessible dropdown.
	 * choices: [{ value, label: () => text, icon: () => node }]
	 */
	function createDropdown(labelKey, choices, current, onSelect) {
		var id = "dropdown-" + (++dropdownCount);
		var wrapper = element("div", { "class": "dropdown" });
		var button = element("button", {
			type: "button",
			"class": "dropdown-button",
			"aria-haspopup": "listbox",
			"aria-expanded": "false",
			"aria-controls": id
		});
		var menu = element("ul", { "class": "dropdown-menu", id: id, role: "listbox", tabindex: "-1", hidden: "" });
		var options = choices.map(function (choice, index) {
			var option = element("li", { "class": "dropdown-option", role: "option", id: id + "-" + index });
			option.addEventListener("click", function () {
				select(choice.value);
			});
			option.addEventListener("mousemove", function () {
				setActive(index);
			});
			menu.appendChild(option);
			return option;
		});
		var activeIndex = 0;

		function isOpen() {
			return !menu.hidden;
		}

		function setActive(index) {
			activeIndex = (index + options.length) % options.length;
			options.forEach(function (option, i) {
				option.classList.toggle("active", i === activeIndex);
			});
			menu.setAttribute("aria-activedescendant", options[activeIndex].id);
		}

		function open() {
			dropdowns.forEach(function (other) {
				if (other.close !== close) {
					other.close(false);
				}
			});
			menu.hidden = false;
			wrapper.classList.add("open");
			button.setAttribute("aria-expanded", "true");
			setActive(Math.max(0, choices.findIndex(function (choice) { return choice.value === current(); })));
			menu.focus();
		}

		function close(focusButton) {
			if (!isOpen()) {
				return;
			}
			menu.hidden = true;
			wrapper.classList.remove("open");
			button.setAttribute("aria-expanded", "false");
			if (focusButton) {
				button.focus();
			}
		}

		function select(value) {
			onSelect(value);
			close(true);
		}

		function update() {
			var selected = choices.find(function (choice) { return choice.value === current(); }) || choices[0];
			button.setAttribute("aria-label", t(labelKey) + ": " + selected.label());
			menu.setAttribute("aria-label", t(labelKey));
			button.textContent = "";
			button.appendChild(selected.icon());
			button.appendChild(element("span", { text: selected.label() }));
			button.appendChild(svg("chevron"));
			choices.forEach(function (choice, index) {
				var option = options[index];
				option.textContent = "";
				option.setAttribute("aria-selected", String(choice === selected));
				option.appendChild(choice.icon());
				option.appendChild(element("span", { text: choice.label() }));
				option.appendChild(svg("check"));
			});
		}

		button.addEventListener("click", function () {
			if (isOpen()) {
				close(false);
			} else {
				open();
			}
		});
		button.addEventListener("keydown", function (event) {
			if (event.key === "ArrowDown" || event.key === "ArrowUp") {
				event.preventDefault();
				open();
			}
		});
		menu.addEventListener("keydown", function (event) {
			switch (event.key) {
				case "ArrowDown":
					setActive(activeIndex + 1);
					break;
				case "ArrowUp":
					setActive(activeIndex - 1);
					break;
				case "Home":
					setActive(0);
					break;
				case "End":
					setActive(options.length - 1);
					break;
				case "Enter":
				case " ":
					select(choices[activeIndex].value);
					break;
				case "Escape":
					close(true);
					break;
				case "Tab":
					close(false);
					return;
				default:
					return;
			}
			event.preventDefault();
		});

		wrapper.appendChild(button);
		wrapper.appendChild(menu);
		var dropdown = { element: wrapper, update: update, close: close, contains: function (node) { return wrapper.contains(node); } };
		dropdowns.push(dropdown);
		update();
		return dropdown;
	}

	document.addEventListener("click", function (event) {
		dropdowns.forEach(function (dropdown) {
			if (!dropdown.contains(event.target)) {
				dropdown.close(false);
			}
		});
	});

	function updateDropdowns() {
		dropdowns.forEach(function (dropdown) {
			dropdown.update();
		});
	}

	function renderTopbar(container) {
		var brand = element("a", { "class": "brand", href: "/" }, [
			element("img", { src: SITE.icon, alt: "", width: "32", height: "32" }),
			element("span", { text: SITE.name })
		]);

		var themes = createDropdown("theme.label", ["system", "dark", "light"].map(function (value) {
			return {
				value: value,
				label: function () { return t("theme." + value); },
				icon: function () { return svg(value); }
			};
		}), function () {
			return document.documentElement.getAttribute("data-theme-setting") || "system";
		}, setTheme);

		// Language names are shown in their own language, so everyone finds theirs
		var langs = createDropdown("lang.label", [
			{ value: "de", label: function () { return "Deutsch"; }, icon: function () { return flag("/assets/img/flag-de.svg"); } },
			{ value: "en", label: function () { return "English"; }, icon: function () { return flag("/assets/img/flag-gb.svg"); } }
		], lang, setLang);

		container.appendChild(brand);
		container.appendChild(element("div", { "class": "controls" }, [themes.element, langs.element]));
	}

	function renderModList(container) {
		var current = document.body.getAttribute("data-mod");
		TYPES.forEach(function (type) {
			var mods = MODS.filter(function (mod) { return mod.type === type.id; });
			if (mods.length === 0) {
				return;
			}
			container.appendChild(element("div", { "class": "modlist-title", "data-i18n": type.title }));
			mods.forEach(function (mod) {
				var attributes = { "class": "mod-entry", href: mod.page };
				if (mod.id === current) {
					attributes["aria-current"] = "page";
				}
				container.appendChild(element("a", attributes, [
					element("img", { src: mod.icon, alt: "", width: "40", height: "40" }),
					element("span", {}, [
						element("span", { "class": "name", text: mod.name }),
						element("span", { "class": "version", "data-mod-version": mod.id, text: "…" })
					])
				]));
			});
		});
	}

	function renderFooter(container) {
		container.appendChild(element("div", { "class": "disclaimer", "data-i18n": "footer.disclaimer" }));
		container.appendChild(element("div", { "data-i18n": "footer.trademark" }));
		container.appendChild(element("div", { "class": "links" }, [
			element("a", { href: "/privacy/", "data-i18n": "footer.privacy" }),
			element("a", { href: SITE.github, rel: "noopener", text: "GitHub" }),
			element("a", { href: SITE.modrinth, rel: "noopener", text: "Modrinth" }),
			element("a", { href: "/assets/fonts/Monocraft-OFL.txt", "data-i18n": "footer.font" }),
			element("span", { text: "© " + new Date().getFullYear() + " " + SITE.name })
		]));
	}

	function init() {
		var topbar = document.getElementById("topbar");
		var modlist = document.getElementById("modlist");
		var footer = document.getElementById("footer");
		if (topbar) {
			renderTopbar(topbar);
		}
		if (modlist) {
			renderModList(modlist);
		}
		if (footer) {
			renderFooter(footer);
		}
		applyTheme(document.documentElement.getAttribute("data-theme-setting") || "system");
		applyLang(lang());
	}

	window.Avie29 = { MODS: MODS, t: t, lang: lang, element: element, applyTranslations: applyTranslations };

	if (document.readyState === "loading") {
		document.addEventListener("DOMContentLoaded", init);
	} else {
		init();
	}
})();

// Loads projects and versions from the Modrinth API and renders release info and download lists.
// Everything updates by itself when a new version is uploaded to Modrinth.
(function () {
	"use strict";

	var API = "https://api.modrinth.com/v2";
	var CACHE_MINUTES = 10;
	// Download columns per project type (see type in site.js)
	var LOADERS = {
		mod: [
			{ id: "fabric", name: "Fabric" },
			{ id: "neoforge", name: "NeoForge" },
			{ id: "forge", name: "Forge" }
		],
		datapack: [
			{ id: "datapack", name: "Datapack" }
		]
	};

	var site = window.Avie29;
	var t = site.t;
	var el = site.element;

	// ---------------------------------------------------------------- API with a short cache

	var pending = {};

	/** Resolves to the parsed JSON, or null when the project does not exist (404). */
	function getJson(path) {
		var cacheKey = "avie29.modrinth:" + path;
		try {
			var cached = JSON.parse(sessionStorage.getItem(cacheKey) || "null");
			if (cached && Date.now() - cached.time < CACHE_MINUTES * 60000) {
				return Promise.resolve(cached.data);
			}
		} catch (e) {
			// No storage available, just load it
		}
		if (!pending[path]) {
			pending[path] = fetch(API + path, { headers: { Accept: "application/json" } }).then(function (response) {
				if (response.status === 404) {
					return null;
				}
				if (!response.ok) {
					throw new Error("Modrinth answered " + response.status);
				}
				return response.json();
			}).then(function (data) {
				try {
					sessionStorage.setItem(cacheKey, JSON.stringify({ time: Date.now(), data: data }));
				} catch (e) {
					// Storage full or blocked, no cache then
				}
				return data;
			});
		}
		return pending[path];
	}

	function loadMod(mod) {
		if (!mod.slug) {
			return Promise.resolve({ project: null, versions: null });
		}
		var slug = encodeURIComponent(mod.slug);
		return Promise.all([getJson("/project/" + slug), getJson("/project/" + slug + "/version")]).then(function (result) {
			return { project: result[0], versions: result[0] ? (result[1] || []) : null };
		});
	}

	// ---------------------------------------------------------------- version logic

	/** Only real releases like 1.21.1 or 26.3, no snapshots or pre-releases. */
	function isReleaseMc(version) {
		return /^\d+(\.\d+)+$/.test(version);
	}

	/** Newest Minecraft version first. */
	function compareMcDesc(a, b) {
		var pa = a.split(".").map(Number);
		var pb = b.split(".").map(Number);
		for (var i = 0; i < Math.max(pa.length, pb.length); i++) {
			var diff = (pb[i] || 0) - (pa[i] || 0);
			if (diff !== 0) {
				return diff;
			}
		}
		return 0;
	}

	/** "1.0.1+26.3" and "v0.2.0+26.2" become "1.0.1" / "0.2.0", other schemes stay as they are. */
	function displayVersion(versionNumber) {
		var match = /^v?(\d+\.\d+\.\d+)(?:[+-].*)?$/.exec(versionNumber);
		return match ? match[1] : versionNumber;
	}

	function loadersOf(mod) {
		return LOADERS[mod.type] || LOADERS.mod;
	}

	/** Does the version run on one of the loaders this project is listed for? */
	function isProjectVersion(mod, version) {
		return loadersOf(mod).some(function (loader) { return version.loaders.indexOf(loader.id) >= 0; });
	}

	function newestFirst(a, b) {
		return Date.parse(b.date_published) - Date.parse(a.date_published);
	}

	/** Latest release of the mod (beta / alpha only when there is no release at all). */
	function latestVersion(mod, versions) {
		var mods = versions.filter(function (version) { return isProjectVersion(mod, version); }).sort(newestFirst);
		return mods.find(function (v) { return v.version_type === "release"; }) || mods[0] || null;
	}

	/** Is candidate a better download for the same loader and Minecraft version than current? */
	function isBetter(candidate, current) {
		var candidateRelease = candidate.version_type === "release";
		var currentRelease = current.version_type === "release";
		if (candidateRelease !== currentRelease) {
			return candidateRelease;
		}
		return Date.parse(candidate.date_published) > Date.parse(current.date_published);
	}

	/**
	 * For one loader: the best version per Minecraft version, newest Minecraft version first.
	 * Neighbouring Minecraft versions that share the same file are merged into one row ("1.21 - 1.21.5").
	 */
	function downloadRows(versions, loader) {
		var best = {};
		versions.forEach(function (version) {
			if (version.loaders.indexOf(loader) < 0) {
				return;
			}
			version.game_versions.filter(isReleaseMc).forEach(function (mc) {
				if (!best[mc] || isBetter(version, best[mc])) {
					best[mc] = version;
				}
			});
		});

		var rows = [];
		Object.keys(best).sort(compareMcDesc).forEach(function (mc) {
			var version = best[mc];
			var last = rows[rows.length - 1];
			if (last && last.version.id === version.id) {
				last.mcs.push(mc);
			} else {
				rows.push({ version: version, mcs: [mc] });
			}
		});
		return rows;
	}

	function primaryFile(version) {
		return version.files.find(function (file) { return file.primary; }) || version.files[0];
	}

	function mcLabel(mcs) {
		// mcs is sorted newest first
		return mcs.length === 1 ? mcs[0] : mcs[mcs.length - 1] + " – " + mcs[0];
	}

	function formatDate(iso) {
		return new Date(iso).toLocaleDateString(site.lang() === "de" ? "de-DE" : "en-GB",
			{ year: "numeric", month: "long", day: "numeric" });
	}

	function formatNumber(value) {
		return Number(value).toLocaleString(site.lang() === "de" ? "de-DE" : "en-GB");
	}

	function modrinthUrl(mod) {
		return "https://modrinth.com/project/" + encodeURIComponent(mod.slug);
	}

	function typeBadge(version) {
		if (version.version_type === "release") {
			return null;
		}
		return el("span", { "class": "badge " + version.version_type, text: t("type." + version.version_type) });
	}

	// ---------------------------------------------------------------- rendering

	function renderSidebar(mod, data) {
		document.querySelectorAll('[data-mod-version="' + mod.id + '"]').forEach(function (target) {
			var latest = data && data.versions ? latestVersion(mod, data.versions) : null;
			target.textContent = latest ? displayVersion(latest.version_number) : "–";
		});
	}

	function renderLatestCard(mod, data) {
		var card = el("div", { "class": "release-card panel" }, [
			el("div", { "class": "head" }, [
				el("img", { src: mod.icon, alt: "", width: "40", height: "40" }),
				el("span", { "class": "title", text: mod.name })
			])
		]);

		if (mod.tagline) {
			card.appendChild(el("div", { "class": "tagline", text: mod.tagline[site.lang()] || mod.tagline.en }));
		}

		if (!data || !data.versions) {
			card.appendChild(el("div", { "class": "meta", text: data === undefined ? t("modrinth.error") : t("modrinth.missing") }));
		} else {
			var latest = latestVersion(mod, data.versions);
			if (latest) {
				var loaders = loadersOf(mod).filter(function (loader) {
					return data.versions.some(function (v) { return v.loaders.indexOf(loader.id) >= 0; });
				});
				card.appendChild(el("div", { "class": "big-version", text: displayVersion(latest.version_number) }));
				card.appendChild(el("div", { "class": "meta", text: t("latest.published", { date: formatDate(latest.date_published) }) }));
				card.appendChild(el("div", { "class": "meta", text: t("latest.downloads", { count: formatNumber(data.project.downloads) }) }));
				card.appendChild(el("div", { "class": "badges" }, loaders.map(function (loader) {
					return el("span", { "class": "badge", text: loader.name });
				})));
			}
		}

		card.appendChild(el("a", { "class": "mc-button small", href: mod.page, text: t(mod.type === "datapack" ? "latest.open.datapack" : "latest.open") }));
		return card;
	}

	function renderDownloads(container, mod, data) {
		container.innerHTML = "";
		if (data === undefined) {
			container.appendChild(el("p", { "class": "error", text: t("modrinth.error") }));
			container.appendChild(el("a", { "class": "mc-button small", href: modrinthUrl(mod), rel: "noopener", text: t("link.modrinth") }));
			return;
		}
		if (!data || !data.versions) {
			container.appendChild(el("p", { "class": "empty", text: t("modrinth.missing") }));
			return;
		}

		var loaders = loadersOf(mod);
		var grid = el("div", { "class": loaders.length === 1 ? "loader-grid single" : "loader-grid" });
		loaders.forEach(function (loader) {
			var column = el("section", { "class": "loader-column panel" }, [el("h3", { text: loader.name })]);
			var rows = downloadRows(data.versions, loader.id);
			if (rows.length === 0) {
				column.appendChild(el("div", { "class": "empty", text: t("downloads.none", { loader: loader.name }) }));
			}
			rows.forEach(function (row) {
				var file = primaryFile(row.version);
				column.appendChild(el("div", { "class": "version-row" }, [
					el("div", {}, [
						el("div", { "class": "mc", text: mcLabel(row.mcs) }),
						el("div", { "class": "mod" }, [
							document.createTextNode(displayVersion(row.version.version_number) + " "),
							typeBadge(row.version)
						])
					]),
					el("a", {
						"class": "mc-button small primary",
						href: file.url,
						title: file.filename,
						rel: "nofollow noopener",
						text: "⬇ " + t("downloads.download")
					})
				]));
			});
			grid.appendChild(column);
		});
		container.appendChild(grid);
		container.appendChild(el("p", {}, [
			el("a", { href: modrinthUrl(mod) + "/versions", rel: "noopener", text: t("downloads.all") })
		]));
	}

	// ---------------------------------------------------------------- description (Markdown from Modrinth)

	// Images in descriptions are only loaded from Modrinth itself (see the privacy policy), others become a link
	var IMAGE_HOSTS = ["cdn.modrinth.com"];

	function isAllowedImage(src) {
		try {
			var url = new URL(src, location.href);
			return url.protocol === "https:" && IMAGE_HOSTS.indexOf(url.hostname) >= 0;
		} catch (e) {
			return false;
		}
	}

	var purifyReady = false;

	function setUpPurify() {
		if (purifyReady) {
			return;
		}
		purifyReady = true;
		// Runs on the inert copy while sanitizing, so blocked images are never requested
		window.DOMPurify.addHook("afterSanitizeAttributes", function (node) {
			if (node.tagName === "IMG") {
				node.removeAttribute("srcset");
				if (isAllowedImage(node.getAttribute("src") || "")) {
					node.setAttribute("loading", "lazy");
					node.setAttribute("decoding", "async");
					node.setAttribute("referrerpolicy", "no-referrer");
				} else {
					node.setAttribute("data-blocked-src", node.getAttribute("src") || "");
					node.removeAttribute("src");
				}
			} else if (node.tagName === "A" && node.hasAttribute("href")) {
				node.setAttribute("rel", "nofollow noopener ugc");
				node.removeAttribute("target");
			}
		});
	}

	/** Modrinth descriptions are Markdown with HTML mixed in; returns a safe DOM fragment. */
	function markdownToFragment(markdown) {
		setUpPurify();
		var fragment = window.DOMPurify.sanitize(window.marked.parse(markdown, { gfm: true }), {
			RETURN_DOM_FRAGMENT: true,
			FORBID_TAGS: ["style", "form", "input", "button", "textarea", "select", "video", "audio", "source", "track", "picture"],
			FORBID_ATTR: ["style"]
		});
		fragment.querySelectorAll("img[data-blocked-src]").forEach(function (img) {
			img.replaceWith(el("a", {
				href: img.getAttribute("data-blocked-src"),
				rel: "nofollow noopener ugc",
				text: img.getAttribute("alt") || t("description.image")
			}));
		});
		return fragment;
	}

	/**
	 * [data-description="<mod id>"] holds a fallback text in both languages.
	 * It is replaced by the Modrinth description once that is loaded, and shown again if there is none.
	 */
	function renderDescription(container, mod, data) {
		if (container.getAttribute("data-rendered") === "true") {
			return;
		}
		var output = container.querySelector(".md");
		if (!output) {
			output = el("div", { "class": "md" });
			container.appendChild(output);
		}
		output.textContent = "";
		container.classList.remove("is-loading");

		var body = data && data.project && data.project.body;
		if (!body || !window.marked || !window.DOMPurify) {
			container.classList.remove("from-modrinth");
			return;
		}
		try {
			output.appendChild(markdownToFragment(body));
		} catch (error) {
			console.warn("Could not show the Modrinth description of " + mod.slug, error);
			output.textContent = "";
			return;
		}
		container.classList.add("from-modrinth");
		// The text itself does not depend on the site language, so it is only drawn once
		container.setAttribute("data-rendered", "true");
	}

	// ---------------------------------------------------------------- gallery (images from Modrinth)

	var ARROWS = {
		prev: '<svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M10 3 5 8l5 5"/></svg>',
		next: '<svg viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M6 3l5 5-5 5"/></svg>'
	};

	var galleries = {};

	function galleryImages(data) {
		var images = data && data.project && data.project.gallery ? data.project.gallery.slice() : [];
		return images.sort(function (a, b) {
			return (a.ordering - b.ordering) || (Date.parse(a.created) - Date.parse(b.created));
		});
	}

	function navButton(direction) {
		var button = el("button", { type: "button", "class": "gallery-nav " + direction });
		button.innerHTML = ARROWS[direction];
		return button;
	}

	/** One big image with buttons to step through, thumbnails below. Returns { update } for language changes. */
	function buildGallery(section, images) {
		var index = 0;
		var image = el("img", { "class": "gallery-image", alt: "", decoding: "async", referrerpolicy: "no-referrer" });
		var full = el("a", { "class": "gallery-full", target: "_blank", rel: "noopener" }, [image]);
		var prev = navButton("prev");
		var next = navButton("next");
		var stage = el("div", { "class": "gallery-stage" }, [full, prev, next]);
		var caption = el("div", { "class": "gallery-caption" });
		var counter = el("div", { "class": "gallery-counter", "aria-live": "polite" });
		var thumbs = el("div", { "class": "gallery-thumbs" });
		var gallery = el("div", {
			"class": "gallery panel" + (images.length === 1 ? " single" : ""),
			role: "region",
			"aria-roledescription": "carousel",
			tabindex: "0"
		}, [stage, el("div", { "class": "gallery-bar" }, [caption, counter]), thumbs]);

		var thumbButtons = images.map(function (item, i) {
			var button = el("button", { type: "button", "class": "gallery-thumb" }, [
				el("img", { src: item.url, alt: "", loading: "lazy", decoding: "async", referrerpolicy: "no-referrer" })
			]);
			button.addEventListener("click", function () {
				show(i);
			});
			thumbs.appendChild(button);
			return button;
		});

		function source(item) {
			return item.raw_url || item.url;
		}

		function show(i) {
			index = (i + images.length) % images.length;
			var item = images[index];
			if (image.getAttribute("src") !== source(item)) {
				stage.classList.add("is-loading");
				image.classList.remove("pixelated");
				image.src = source(item);
				full.href = source(item);
			}
			caption.textContent = "";
			if (item.title) {
				caption.appendChild(el("strong", { text: item.title }));
			}
			if (item.description) {
				caption.appendChild(el("span", { text: item.description }));
			}
			update();
		}

		function update() {
			var item = images[index];
			image.alt = item.title || item.description || t("gallery.image", { n: index + 1 });
			gallery.setAttribute("aria-label", t("gallery.title"));
			full.setAttribute("title", t("gallery.full"));
			prev.setAttribute("aria-label", t("gallery.prev"));
			next.setAttribute("aria-label", t("gallery.next"));
			counter.textContent = t("gallery.count", { n: index + 1, total: images.length });
			thumbButtons.forEach(function (button, i) {
				button.setAttribute("aria-label", t("gallery.show", { n: i + 1 }));
				button.setAttribute("aria-current", i === index ? "true" : "false");
			});
		}

		image.addEventListener("load", function () {
			stage.classList.remove("is-loading");
			// Small screenshots are scaled up like the game does it: sharp pixels instead of a blur
			var scale = Math.min(stage.clientWidth / image.naturalWidth, stage.clientHeight / image.naturalHeight);
			image.classList.toggle("pixelated", scale > 1.5);
			// Load the next image in the background so stepping through feels instant
			if (images.length > 1) {
				new Image().src = source(images[(index + 1) % images.length]);
			}
		});
		image.addEventListener("error", function () {
			stage.classList.remove("is-loading");
		});

		prev.addEventListener("click", function () {
			show(index - 1);
		});
		next.addEventListener("click", function () {
			show(index + 1);
		});
		gallery.addEventListener("keydown", function (event) {
			if (images.length < 2) {
				return;
			}
			if (event.key === "ArrowLeft") {
				show(index - 1);
			} else if (event.key === "ArrowRight") {
				show(index + 1);
			} else {
				return;
			}
			event.preventDefault();
		});

		// Swipe on touch screens
		var touchX = null;
		stage.addEventListener("touchstart", function (event) {
			touchX = event.touches.length === 1 ? event.touches[0].clientX : null;
		}, { passive: true });
		stage.addEventListener("touchend", function (event) {
			if (touchX === null) {
				return;
			}
			var distance = event.changedTouches[0].clientX - touchX;
			touchX = null;
			if (Math.abs(distance) > 40 && images.length > 1) {
				show(distance < 0 ? index + 1 : index - 1);
			}
		});

		section.appendChild(gallery);
		show(0);
		return { update: update };
	}

	/** [data-gallery="<mod id>"] stays hidden unless the project has images on Modrinth. */
	function renderGallery(section, mod, data) {
		if (galleries[mod.id]) {
			galleries[mod.id].update();
			return;
		}
		var images = galleryImages(data);
		section.hidden = images.length === 0;
		if (images.length > 0) {
			galleries[mod.id] = buildGallery(section, images);
		}
	}

	// ---------------------------------------------------------------- page wiring

	var results = {};

	function redraw() {
		site.MODS.forEach(function (mod) {
			if (mod.id in results) {
				renderSidebar(mod, results[mod.id]);
			}
		});

		// data-latest-releases="mod" / "datapack" shows only that type, an empty value shows everything
		document.querySelectorAll("[data-latest-releases]").forEach(function (container) {
			var type = container.getAttribute("data-latest-releases");
			container.innerHTML = "";
			var cards = el("div", { "class": "release-cards" });
			site.MODS.forEach(function (mod) {
				if (mod.id in results && (!type || mod.type === type)) {
					cards.appendChild(renderLatestCard(mod, results[mod.id]));
				}
			});
			container.appendChild(cards);
		});

		document.querySelectorAll("[data-downloads]").forEach(function (container) {
			var mod = site.MODS.find(function (m) { return m.id === container.getAttribute("data-downloads"); });
			if (mod && mod.id in results) {
				renderDownloads(container, mod, results[mod.id]);
			}
		});

		document.querySelectorAll("[data-description]").forEach(function (container) {
			var mod = site.MODS.find(function (m) { return m.id === container.getAttribute("data-description"); });
			if (mod && mod.id in results) {
				renderDescription(container, mod, results[mod.id]);
			}
		});

		document.querySelectorAll("[data-gallery]").forEach(function (section) {
			var mod = site.MODS.find(function (m) { return m.id === section.getAttribute("data-gallery"); });
			if (mod && mod.id in results) {
				renderGallery(section, mod, results[mod.id]);
			}
		});

		document.querySelectorAll("[data-modrinth-link]").forEach(function (link) {
			var mod = site.MODS.find(function (m) { return m.id === link.getAttribute("data-modrinth-link"); });
			if (mod && mod.slug) {
				link.href = modrinthUrl(mod);
			} else {
				link.hidden = true;
			}
		});
	}

	function showLoading() {
		document.querySelectorAll("[data-latest-releases], [data-downloads]").forEach(function (container) {
			container.innerHTML = "";
			container.appendChild(el("div", { "class": "loading", "data-i18n": "modrinth.loading", text: t("modrinth.loading") }));
		});
		// The fallback text stays in the page (for search engines and without JavaScript), it is only hidden while loading
		document.querySelectorAll("[data-description]").forEach(function (container) {
			container.classList.add("is-loading");
			container.appendChild(el("div", { "class": "md" }, [
				el("div", { "class": "loading", "data-i18n": "modrinth.loading", text: t("modrinth.loading") })
			]));
		});
	}

	function start() {
		showLoading();
		redraw();
		site.MODS.forEach(function (mod) {
			loadMod(mod).then(function (data) {
				results[mod.id] = data;
			}, function (error) {
				console.warn("Modrinth request for " + mod.slug + " failed", error);
				// undefined = could not load, null = project does not exist
				results[mod.id] = undefined;
			}).then(redraw);
		});
		document.addEventListener("avie29:lang", redraw);
	}

	start();
})();

#!/usr/bin/env python3
"""
Genera los recursos de texto del modulo compartido a partir de los de Android.

Por que existe: la app Android ya esta traducida a cinco idiomas por personas, y
la interfaz compartida necesita los mismos textos. Copiarlos a mano invitaria a
que las dos versiones divergieran y a inventar traducciones peores que las que ya
hay. Este script toma la traduccion real de `app/src/main/res/values*/` y escribe
`shared/src/commonMain/composeResources/values*/strings.xml`.

Se vuelve a ejecutar cuando cambien los textos de Android. Informa de toda clave
que no encuentre en vez de escribir el hueco en silencio.

Uso:  python3 -s docs/generar_textos_compartidos.py
"""

from __future__ import annotations

import pathlib
import re
import sys
import xml.sax.saxutils as sax

RAIZ = pathlib.Path(__file__).resolve().parent.parent
ANDROID_RES = RAIZ / "app/src/main/res"
DESTINO = RAIZ / "shared/src/commonMain/composeResources"

IDIOMAS = ["", "-en", "-de", "-fr", "-pt"]

# clave compartida -> clave de Android que ya esta traducida
DESDE_ANDROID = {
    "focus_title": "focus_title",
    "focus_pause": "focus_pause",
    "focus_resume": "focus_resume",
    "focus_end": "focus_finish",
    "focus_skip_break": "focus_skip_break",
    "focus_status_running": "focus_status_running",
    "focus_status_break": "focus_status_break",
    "focus_status_paused": "focus_status_paused",
    "focus_status_done": "focus_status_done",
    "focus_preset_pomodoro": "focus_preset_pomodoro",
    "focus_custom_create": "focus_create_custom",
    "focus_duration_label": "focus_duration",
    "focus_break_label": "focus_break",
    "action_save": "action_save",
    "action_cancel": "action_cancel",
    "birth_date_title": "birth_date_title",
    "gam_level_short": "community_level_short",
    "gam_level_label": "community_stat_level",
    "gam_streak_label": "community_stat_streak",
    "gam_xp_to_next": "gamification_header_xp_to_next_level",
    "tab_settings": "nav_settings",
    "settings_life_expectancy": "life_expectancy_title",
    "settings_life_expectancy_hint": "life_expectancy_hint",
    "settings_theme_light": "theme_mode_light",
    "settings_theme_dark": "theme_mode_dark",
    "settings_theme_system": "theme_mode_system",
    "settings_version": "about_info_version_title",
}

# Textos que no existen en Android, traducidos aqui. El orden de la tupla sigue
# a IDIOMAS: es, en, de, fr, pt.
PROPIOS = {
    "focus_idle_hint": (
        "Elige cuánto quieres concentrarte",
        "Choose how long to focus",
        "Wähle deine Fokusdauer",
        "Choisis ta durée de concentration",
        "Escolhe quanto tempo focar",
    ),
    "focus_preset_long": (
        "Sesión larga", "Long session", "Lange Sitzung",
        "Session longue", "Sessão longa",
    ),
    "focus_preset_sprint": (
        "Sprint corto", "Short sprint", "Kurzer Sprint",
        "Sprint court", "Sprint curto",
    ),
    "focus_preset_own": (
        "Sesión propia", "My session", "Eigene Sitzung",
        "Ma session", "Sessão própria",
    ),
    "focus_break_paused": (
        "Descanso en pausa", "Break paused", "Pause angehalten",
        "Pause suspendue", "Descanso pausado",
    ),
    "focus_custom_title": (
        "Sesión a tu medida", "Your own session", "Deine eigene Sitzung",
        "Ta session sur mesure", "Sessão à tua medida",
    ),
    "focus_custom_open": (
        "Sesión a mi medida", "Custom session", "Eigene Sitzung",
        "Session sur mesure", "Sessão à minha medida",
    ),
    "focus_no_break": (
        "Sin descanso", "No break", "Keine Pause",
        "Sans pause", "Sem descanso",
    ),
    "focus_close": ("Cerrar", "Close", "Schließen", "Fermer", "Fechar"),
    "action_start": ("Empezar", "Start", "Starten", "Commencer", "Começar"),
    "life_title": (
        "Mi vida en semanas", "My life in weeks", "Mein Leben in Wochen",
        "Ma vie en semaines", "A minha vida em semanas",
    ),
    "life_empty_hint": (
        "Cada casilla es una semana de tu vida. Para dibujarla hace falta saber cuándo empezaste.",
        "Each square is one week of your life. To draw it we need to know when you started.",
        "Jedes Kästchen ist eine Woche deines Lebens. Dafür müssen wir wissen, wann du angefangen hast.",
        "Chaque case est une semaine de ta vie. Pour la dessiner, il faut savoir quand tu as commencé.",
        "Cada quadrado é uma semana da tua vida. Para a desenhar precisamos de saber quando começaste.",
    ),
    "life_pick_birth": (
        "Elegir mi fecha de nacimiento", "Choose my date of birth",
        "Geburtsdatum wählen", "Choisir ma date de naissance",
        "Escolher a minha data de nascimento",
    ),
    "life_change_birth": (
        "Cambiar fecha de nacimiento", "Change date of birth",
        "Geburtsdatum ändern", "Changer la date de naissance",
        "Alterar data de nascimento",
    ),
    "life_years": ("Años", "Years", "Jahre", "Ans", "Anos"),
    "life_weeks_lived": (
        "Semanas vividas", "Weeks lived", "Gelebte Wochen",
        "Semaines vécues", "Semanas vividas",
    ),
    "life_weeks_left": (
        "Semanas por vivir", "Weeks to live", "Verbleibende Wochen",
        "Semaines à vivre", "Semanas por viver",
    ),
    "label_year": ("Año", "Year", "Jahr", "Année", "Ano"),
    "label_month": ("Mes", "Month", "Monat", "Mois", "Mês"),
    "label_day": ("Día", "Day", "Tag", "Jour", "Dia"),
    "tab_life": ("Mi vida", "My life", "Mein Leben", "Ma vie", "A minha vida"),
    "notif_focus_done_title": (
        "Sesión de enfoque completada", "Focus session complete",
        "Fokus-Sitzung abgeschlossen", "Session de concentration terminée",
        "Sessão de foco concluída",
    ),
    "notif_break_done_title": (
        "Descanso terminado", "Break over", "Pause beendet",
        "Pause terminée", "Descanso terminado",
    ),
    "notif_focus_done_body": (
        "%1$s de %2$s. Te toca descansar.",
        "%1$s of %2$s. Time for a break.",
        "%1$s von %2$s. Zeit für eine Pause.",
        "%1$s de %2$s. C'est l'heure de la pause.",
        "%1$s de %2$s. É hora de descansar.",
    ),
    "notif_break_done_body": (
        "%1$s de pausa. ¿Otra sesión?",
        "%1$s of break. Another session?",
        "%1$s Pause. Noch eine Sitzung?",
        "%1$s de pause. Une autre session ?",
        "%1$s de pausa. Outra sessão?",
    ),
    "notif_break_resume_body": (
        "Vuelve cuando quieras.", "Come back whenever you like.",
        "Komm zurück, wann du willst.", "Reviens quand tu veux.",
        "Volta quando quiseres.",
    ),
    # La concordancia de numero se resuelve con dos textos y no con uno solo,
    # porque «1 minutos» se cuela en cuanto se compone la cifra a mano.
    "minutes_one": ("1 minuto", "1 minute", "1 Minute", "1 minute", "1 minuto"),
    "minutes_other": (
        "%1$d minutos", "%1$d minutes", "%1$d Minuten",
        "%1$d minutes", "%1$d minutos",
    ),
    "focus_completed_detail": (
        "Sesión completada: %1$s de enfoque.",
        "Session complete: %1$s of focus.",
        "Sitzung abgeschlossen: %1$s Fokus.",
        "Session terminée : %1$s de concentration.",
        "Sessão concluída: %1$s de foco.",
    ),
    "focus_start_break_action": (
        "Empezar descanso de %1$d min", "Start %1$d min break",
        "%1$d-min-Pause starten", "Commencer une pause de %1$d min",
        "Começar descanso de %1$d min",
    ),
    "focus_preset_times": (
        "%1$d min · %2$d min", "%1$d min · %2$d min", "%1$d Min · %2$d Min",
        "%1$d min · %2$d min", "%1$d min · %2$d min",
    ),
    "tab_focus": ("Enfoque", "Focus", "Fokus", "Concentration", "Foco"),
    "settings_appearance": (
        "Apariencia", "Appearance", "Erscheinungsbild", "Apparence", "Aparência",
    ),
    # El texto de monedas de Android trae un emoji (🪙) y el sistema de diseño de
    # Momentum los prohíbe, así que aquí se nombra en palabras.
    "gam_coins_label": ("Monedas", "Coins", "Münzen", "Pièces", "Moedas"),
    "gam_session_earned": (
        "+%1$d XP · +%2$d monedas",
        "+%1$d XP · +%2$d coins",
        "+%1$d XP · +%2$d Münzen",
        "+%1$d XP · +%2$d pièces",
        "+%1$d XP · +%2$d moedas",
    ),
    "gam_level_up": (
        "Has subido al nivel %1$d", "You reached level %1$d",
        "Du hast Level %1$d erreicht", "Tu as atteint le niveau %1$d",
        "Alcançaste o nível %1$d",
    ),
    "gam_streak_days": (
        "%1$d días seguidos", "%1$d days in a row", "%1$d Tage in Folge",
        "%1$d jours d'affilée", "%1$d dias seguidos",
    ),
    "gam_streak_one_day": (
        "Primer día", "First day", "Erster Tag", "Premier jour", "Primeiro dia",
    ),
    "gam_multiplier": (
        "Racha ×%1$s", "Streak ×%1$s", "Serie ×%1$s",
        "Série ×%1$s", "Sequência ×%1$s",
    ),
}


def limpiar(valor: str) -> str:
    """
    Quita el emoji inicial de un texto de Android.

    El sistema de diseño de Momentum prohíbe los emoji, pero algunos textos de la
    app Android los llevan («⏳ Expectativa de Vida»). Se retiran aquí en vez de
    reescribir el texto a mano, para que reutilizar la traducción siga siendo
    automático.
    """
    limpio = valor.strip()
    # Un emoji al principio va seguido de espacio; se corta por ahí sin tocar los
    # textos que empiezan por letra, número o signo de apertura.
    if limpio and not (limpio[0].isalnum() or limpio[0] in "¿¡\"'%+-<({[&#"):
        partes = limpio.split(" ", 1)
        if len(partes) == 2 and partes[1]:
            return partes[1].strip()
    return limpio


def leer_traducciones(sufijo: str) -> dict[str, str]:
    """Todas las claves de un idioma, reunidas de sus varios ficheros."""
    carpeta = ANDROID_RES / f"values{sufijo}"
    encontradas: dict[str, str] = {}
    for fichero in sorted(carpeta.glob("strings*.xml")):
        texto = fichero.read_text(encoding="utf-8")
        for nombre, valor in re.findall(
            r'<string name="([^"]+)"[^>]*>(.*?)</string>', texto, re.DOTALL
        ):
            encontradas.setdefault(nombre, limpiar(valor))
    return encontradas


def main() -> int:
    faltantes: list[str] = []
    for indice, sufijo in enumerate(IDIOMAS):
        android = leer_traducciones(sufijo)
        # El idioma por defecto es el respaldo cuando una clave no esta traducida.
        respaldo = leer_traducciones("") if sufijo else android

        lineas = [
            '<?xml version="1.0" encoding="utf-8"?>',
            "<!-- GENERADO por docs/generar_textos_compartidos.py. No editar a mano:",
            "     los textos vienen de app/src/main/res/values*/ para que Android e iOS",
            "     digan lo mismo. Vuelve a ejecutar el script tras cambiar los de Android. -->",
            "<resources>",
        ]

        for clave, clave_android in DESDE_ANDROID.items():
            valor = android.get(clave_android) or respaldo.get(clave_android)
            if valor is None:
                faltantes.append(f"values{sufijo}: {clave_android}")
                continue
            lineas.append(f'    <string name="{clave}">{valor}</string>')

        for clave, valores in PROPIOS.items():
            lineas.append(
                f'    <string name="{clave}">{sax.escape(valores[indice])}</string>'
            )

        lineas.append("</resources>")

        salida = DESTINO / f"values{sufijo}" / "strings.xml"
        salida.parent.mkdir(parents=True, exist_ok=True)
        salida.write_text("\n".join(lineas) + "\n", encoding="utf-8")
        print(f"escrito {salida.relative_to(RAIZ)}  ({len(lineas) - 5} textos)")

    if faltantes:
        print("\nCLAVES NO ENCONTRADAS (revisar):")
        for f in faltantes:
            print(f"  {f}")
        return 1
    print("\nTodas las claves de Android se resolvieron.")
    return 0


if __name__ == "__main__":
    sys.exit(main())

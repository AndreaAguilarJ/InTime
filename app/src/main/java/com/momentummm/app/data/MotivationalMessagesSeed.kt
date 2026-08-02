package com.momentummm.app.data

import com.momentummm.app.data.entity.MessageCategory
import com.momentummm.app.data.entity.MessageTone
import com.momentummm.app.data.entity.MotivationalMessage
import java.util.Date
import java.util.UUID

/**
 * Seed data for motivational messages.
 * Contains 500+ messages in Spanish distributed across all categories and tones.
 */
object MotivationalMessagesSeed {

    fun getMotivationalMessages(): List<MotivationalMessage> {
        val messages = mutableListOf<MotivationalMessage>()
        val now = Date()
        
        // ============== MORNING MESSAGES (60+) ==============
        messages.addAll(listOf(
            // Friendly tone
            createMessage("Buenos días campeón 🌅 Hoy es el día perfecto para acercarte a tus metas", MessageCategory.MORNING, MessageTone.FRIENDLY, "🌅"),
            createMessage("¡Arriba! ☀️ El sol salió y tú también deberías brillar hoy", MessageCategory.MORNING, MessageTone.FRIENDLY, "☀️"),
            createMessage("Buenos días 🌻 Recuerda que cada mañana es una nueva oportunidad de ser increíble", MessageCategory.MORNING, MessageTone.FRIENDLY, "🌻"),
            createMessage("¡Despierta! 🌈 Hoy tiene todo el potencial de ser tu mejor día", MessageCategory.MORNING, MessageTone.FRIENDLY, "🌈"),
            createMessage("Buenos días ✨ Tienes 24 horas nuevecitas para crear magia", MessageCategory.MORNING, MessageTone.FRIENDLY, "✨"),
            createMessage("¡Hola día nuevo! 🌞 Estás listo para conquistarlo", MessageCategory.MORNING, MessageTone.FRIENDLY, "🌞"),
            createMessage("Buenos días guerrero 💪 El mundo te espera", MessageCategory.MORNING, MessageTone.FRIENDLY, "💪"),
            createMessage("¡Arriba y adelante! 🚀 Hoy escribes un nuevo capítulo de tu historia", MessageCategory.MORNING, MessageTone.FRIENDLY, "🚀"),
            
            // Coach tone
            createMessage("Es hora de levantarse 💪 Los campeones no se quedan en cama", MessageCategory.MORNING, MessageTone.COACH, "💪"),
            createMessage("Nuevo día, nuevas oportunidades 🎯 ¿Cuál es tu primera victoria hoy?", MessageCategory.MORNING, MessageTone.COACH, "🎯"),
            createMessage("Buenos días 🔥 Tu competencia ya está trabajando. ¿Qué esperas?", MessageCategory.MORNING, MessageTone.COACH, "🔥"),
            createMessage("Mientras otros duermen, los exitosos ya están en acción 💯", MessageCategory.MORNING, MessageTone.COACH, "💯"),
            createMessage("Hoy es el día para demostrar de qué estás hecho 🏆", MessageCategory.MORNING, MessageTone.COACH, "🏆"),
            createMessage("Despierta con propósito 🎯 Cada minuto cuenta", MessageCategory.MORNING, MessageTone.COACH, "🎯"),
            createMessage("Buenos días atleta 🏃 Es hora de entrenar tu mente y cuerpo", MessageCategory.MORNING, MessageTone.COACH, "🏃"),
            createMessage("¡Sin excusas! 💪 Hoy empiezas más fuerte que ayer", MessageCategory.MORNING, MessageTone.COACH, "💪"),
            
            // Wise tone
            createMessage("Buenos días 🌅 El amanecer nos recuerda que siempre hay nuevos comienzos", MessageCategory.MORNING, MessageTone.WISE, "🌅"),
            createMessage("Cada mañana naces de nuevo. Lo que hagas hoy es lo que más importa 🧘", MessageCategory.MORNING, MessageTone.WISE, "🧘"),
            createMessage("El día más importante de tu vida es hoy 📿 Vívelo con intención", MessageCategory.MORNING, MessageTone.WISE, "📿"),
            createMessage("Buenos días 🌸 La paz interior comienza con una mañana consciente", MessageCategory.MORNING, MessageTone.WISE, "🌸"),
            createMessage("Despierta agradecido 🙏 La gratitud transforma días ordinarios en extraordinarios", MessageCategory.MORNING, MessageTone.WISE, "🙏"),
            createMessage("Buenos días 🌿 Respira profundo. Este momento es todo lo que tienes", MessageCategory.MORNING, MessageTone.WISE, "🌿"),
            createMessage("El silencio de la mañana es perfecto para escuchar tu sabiduría interior 🧘‍♂️", MessageCategory.MORNING, MessageTone.WISE, "🧘‍♂️"),
            createMessage("Buenos días viajero 🌄 Otro paso en tu camino de crecimiento personal", MessageCategory.MORNING, MessageTone.WISE, "🌄"),
            
            // Energetic tone
            createMessage("¡BUENOS DÍAS! 🌟 ¡Es momento de BRILLAR con toda tu energía!", MessageCategory.MORNING, MessageTone.ENERGETIC, "🌟"),
            createMessage("¡ARRIBA! ⚡ ¡Hoy vas a romperla en todo lo que hagas!", MessageCategory.MORNING, MessageTone.ENERGETIC, "⚡"),
            createMessage("¡WOW! 🎉 ¡Otro día increíble para ser INCREÍBLE!", MessageCategory.MORNING, MessageTone.ENERGETIC, "🎉"),
            createMessage("¡VAMOS! 🔥🔥🔥 ¡Hoy nada ni nadie te detiene!", MessageCategory.MORNING, MessageTone.ENERGETIC, "🔥"),
            createMessage("¡DESPIERTA LEYENDA! 🦁 ¡El mundo necesita tu grandeza!", MessageCategory.MORNING, MessageTone.ENERGETIC, "🦁"),
            createMessage("¡BOOM! 💥 ¡Buenos días campeón! ¡A conquistar!", MessageCategory.MORNING, MessageTone.ENERGETIC, "💥"),
            createMessage("¡YEAHHH! 🚀 ¡Hoy vas a hacer cosas ÉPICAS!", MessageCategory.MORNING, MessageTone.ENERGETIC, "🚀"),
            createMessage("¡POWER UP! ⚡ ¡Tu día comienza AHORA con toda la fuerza!", MessageCategory.MORNING, MessageTone.ENERGETIC, "⚡"),
            
            // Calm tone
            createMessage("Buenos días 🌸 Despierta suavemente, el día te espera con paciencia", MessageCategory.MORNING, MessageTone.CALM, "🌸"),
            createMessage("Respira 🌿 Este nuevo día trae calma y posibilidades infinitas", MessageCategory.MORNING, MessageTone.CALM, "🌿"),
            createMessage("Buenos días 🌊 Como las olas, fluye con tranquilidad hacia tus metas", MessageCategory.MORNING, MessageTone.CALM, "🌊"),
            createMessage("Abre los ojos gentilmente 🌷 Un nuevo día lleno de serenidad te abraza", MessageCategory.MORNING, MessageTone.CALM, "🌷"),
            createMessage("Buenos días 🕊️ Que la paz guíe cada uno de tus pasos hoy", MessageCategory.MORNING, MessageTone.CALM, "🕊️"),
            createMessage("Despierta con calma 🌙 No hay prisa, solo presencia", MessageCategory.MORNING, MessageTone.CALM, "🌙"),
            createMessage("Buenos días 🍃 El silencio de la mañana es un regalo para tu alma", MessageCategory.MORNING, MessageTone.CALM, "🍃"),
            createMessage("Buen despertar 🌺 Comienza tu día con gratitud y tranquilidad", MessageCategory.MORNING, MessageTone.CALM, "🌺"),
            
            // Humorous tone
            createMessage("Buenos días 😴 Sé que la cama te ama, pero tus metas te necesitan más", MessageCategory.MORNING, MessageTone.HUMOROUS, "😴"),
            createMessage("¡Arriba! ☕ Tu café no se va a tomar solo (o sí, pero eso sería triste)", MessageCategory.MORNING, MessageTone.HUMOROUS, "☕"),
            createMessage("Buenos días 🛏️ La almohada es tu ex, ya supérala y levántate", MessageCategory.MORNING, MessageTone.HUMOROUS, "🛏️"),
            createMessage("¡Hey! 🐓 El gallo ya cantó hace rato, tú también puedes cantar victoria hoy", MessageCategory.MORNING, MessageTone.HUMOROUS, "🐓"),
            createMessage("Buenos días 🌞 Si lees esto acostado... ¡Atrápate levantándote!", MessageCategory.MORNING, MessageTone.HUMOROUS, "🌞"),
            createMessage("¡Arriba! 🍳 Los huevos no se hacen solos (bueno, las gallinas sí, pero tú entiéndes)", MessageCategory.MORNING, MessageTone.HUMOROUS, "🍳"),
            createMessage("Buenos días campeón 🏋️ La pesa más difícil de levantar es la cobija", MessageCategory.MORNING, MessageTone.HUMOROUS, "🏋️"),
            createMessage("¡Despierta! 📱 Este mensaje no se va a leer solo en la cama... o tal vez sí", MessageCategory.MORNING, MessageTone.HUMOROUS, "📱"),
            
            // Inspirational tone
            createMessage("Buenos días 🌟 Hoy tienes la oportunidad de inspirar a alguien, empezando por ti", MessageCategory.MORNING, MessageTone.INSPIRATIONAL, "🌟"),
            createMessage("Despierta soñador 🌈 El universo conspira a favor de quienes creen", MessageCategory.MORNING, MessageTone.INSPIRATIONAL, "🌈"),
            createMessage("Buenos días ✨ Eres capaz de cosas extraordinarias, hoy lo demostrarás", MessageCategory.MORNING, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Abre los ojos 🦋 Un nuevo día lleno de posibilidades infinitas te espera", MessageCategory.MORNING, MessageTone.INSPIRATIONAL, "🦋"),
            createMessage("Buenos días héroe 🌅 Tu historia de éxito continúa hoy", MessageCategory.MORNING, MessageTone.INSPIRATIONAL, "🌅"),
            createMessage("Despierta 💫 Hoy es el día para convertir sueños en realidad", MessageCategory.MORNING, MessageTone.INSPIRATIONAL, "💫"),
            createMessage("Buenos días visionario 🔮 El futuro que imaginas se construye hoy", MessageCategory.MORNING, MessageTone.INSPIRATIONAL, "🔮"),
            createMessage("¡Levántate! 🌻 El mundo necesita la luz única que solo tú puedes dar", MessageCategory.MORNING, MessageTone.INSPIRATIONAL, "🌻"),
            
            // Practical tone
            createMessage("Buenos días 📋 Revisa tus 3 prioridades del día antes de empezar", MessageCategory.MORNING, MessageTone.PRACTICAL, "📋"),
            createMessage("¡Arriba! 💧 Primer paso: un vaso de agua para activar tu metabolismo", MessageCategory.MORNING, MessageTone.PRACTICAL, "💧"),
            createMessage("Buenos días 🎯 Define tu objetivo principal antes de revisar el celular", MessageCategory.MORNING, MessageTone.PRACTICAL, "🎯"),
            createMessage("Hora de levantarse 📝 5 minutos de planificación ahorran 1 hora de caos", MessageCategory.MORNING, MessageTone.PRACTICAL, "📝"),
            createMessage("Buenos días 🏋️ 10 minutos de ejercicio matutino = 3 horas de energía extra", MessageCategory.MORNING, MessageTone.PRACTICAL, "🏋️"),
            createMessage("¡Arriba! 🧘 5 minutos de meditación preparan tu mente para el día", MessageCategory.MORNING, MessageTone.PRACTICAL, "🧘"),
            createMessage("Buenos días 📵 Primeros 30 min sin redes = enfoque garantizado", MessageCategory.MORNING, MessageTone.PRACTICAL, "📵"),
            createMessage("Es hora 🍳 Un desayuno nutritivo es tu primera decisión inteligente del día", MessageCategory.MORNING, MessageTone.PRACTICAL, "🍳")
        ))
        
        // ============== EVENING MESSAGES (50+) ==============
        messages.addAll(listOf(
            createMessage("Buenas noches 🌙 Hoy diste lo mejor de ti, mereces descansar", MessageCategory.EVENING, MessageTone.FRIENDLY, "🌙"),
            createMessage("Hora de descansar 💤 Mañana será otro día increíble", MessageCategory.EVENING, MessageTone.FRIENDLY, "💤"),
            createMessage("Buenas noches campeón 🌟 Sueña con las victorias de mañana", MessageCategory.EVENING, MessageTone.FRIENDLY, "🌟"),
            createMessage("Es hora de recargar 🔋 Tu cuerpo y mente lo necesitan", MessageCategory.EVENING, MessageTone.FRIENDLY, "🔋"),
            createMessage("Dulces sueños 🌸 Mañana te espera con nuevas oportunidades", MessageCategory.EVENING, MessageTone.FRIENDLY, "🌸"),
            createMessage("Buenas noches 🌜 Descansa, lo mereces después de un gran día", MessageCategory.EVENING, MessageTone.FRIENDLY, "🌜"),
            createMessage("Hora de dormir 🛏️ Recarga energías para seguir brillando mañana", MessageCategory.EVENING, MessageTone.FRIENDLY, "🛏️"),
            createMessage("Buenas noches 😴 Cierra los ojos sabiendo que hiciste tu mejor esfuerzo", MessageCategory.EVENING, MessageTone.FRIENDLY, "😴"),
            
            createMessage("Descansa guerrero 💪 Mañana hay más batallas que ganar", MessageCategory.EVENING, MessageTone.COACH, "💪"),
            createMessage("Buenas noches atleta 🏆 La recuperación es parte del entrenamiento", MessageCategory.EVENING, MessageTone.COACH, "🏆"),
            createMessage("Es hora de recuperarte 🎯 Los campeones saben cuándo descansar", MessageCategory.EVENING, MessageTone.COACH, "🎯"),
            createMessage("Buenas noches 🔥 Hoy preparaste el camino para el éxito de mañana", MessageCategory.EVENING, MessageTone.COACH, "🔥"),
            createMessage("Descansa estratégicamente 💯 El sueño es tu arma secreta", MessageCategory.EVENING, MessageTone.COACH, "💯"),
            createMessage("Buenas noches 🏃 8 horas de sueño = rendimiento máximo mañana", MessageCategory.EVENING, MessageTone.COACH, "🏃"),
            
            createMessage("Buenas noches 🌿 Suelta las preocupaciones del día y descansa en paz", MessageCategory.EVENING, MessageTone.WISE, "🌿"),
            createMessage("Es hora de soltar 🙏 El día cumplió su propósito, ahora descansa", MessageCategory.EVENING, MessageTone.WISE, "🙏"),
            createMessage("Buenas noches 🧘 En el silencio nocturno encontramos claridad", MessageCategory.EVENING, MessageTone.WISE, "🧘"),
            createMessage("Descansa tranquilo 🌙 Mañana traerá la sabiduría que necesitas", MessageCategory.EVENING, MessageTone.WISE, "🌙"),
            createMessage("Buenas noches 🌸 El descanso es tan importante como la acción", MessageCategory.EVENING, MessageTone.WISE, "🌸"),
            createMessage("Es hora de soltar 🕊️ Lo que sea que haya pasado hoy, ya pasó", MessageCategory.EVENING, MessageTone.WISE, "🕊️"),
            
            createMessage("Buenas noches 🌊 Deja que el silencio de la noche calme tu mente", MessageCategory.EVENING, MessageTone.CALM, "🌊"),
            createMessage("Respira y descansa 🌿 Tu cuerpo sabe cómo sanarse mientras duermes", MessageCategory.EVENING, MessageTone.CALM, "🌿"),
            createMessage("Buenas noches 🌷 Suelta la tensión, abraza la tranquilidad", MessageCategory.EVENING, MessageTone.CALM, "🌷"),
            createMessage("Es hora de descansar 🍃 Permite que la paz nocturna te envuelva", MessageCategory.EVENING, MessageTone.CALM, "🍃"),
            createMessage("Buenas noches 🌺 Cierra los ojos con gratitud por este día vivido", MessageCategory.EVENING, MessageTone.CALM, "🌺"),
            createMessage("Descansa sereno 🌙 La noche es un abrazo reconfortante", MessageCategory.EVENING, MessageTone.CALM, "🌙"),
            
            createMessage("Buenas noches 😴 No te quedes en el celular, que mañana hay vida", MessageCategory.EVENING, MessageTone.HUMOROUS, "😴"),
            createMessage("¡A dormir! 📱 Netflix estará ahí mañana, tu sueño no", MessageCategory.EVENING, MessageTone.HUMOROUS, "📱"),
            createMessage("Buenas noches 🛏️ La almohada te extraña, no la dejes esperando", MessageCategory.EVENING, MessageTone.HUMOROUS, "🛏️"),
            createMessage("Es hora de soñar 💭 Que tus sueños sean mejores que las series que ves", MessageCategory.EVENING, MessageTone.HUMOROUS, "💭"),
            createMessage("Buenas noches 😂 Deja de scrollear y cierra los ojos ya", MessageCategory.EVENING, MessageTone.HUMOROUS, "😂"),
            createMessage("¡A la cama! 🌙 Tu cuerpo no funciona con WiFi, necesita sueño", MessageCategory.EVENING, MessageTone.HUMOROUS, "🌙"),
            
            createMessage("Buenas noches 💫 Sueña en grande, mañana hazlo realidad", MessageCategory.EVENING, MessageTone.INSPIRATIONAL, "💫"),
            createMessage("Descansa soñador 🌟 Mientras duermes, el universo prepara milagros", MessageCategory.EVENING, MessageTone.INSPIRATIONAL, "🌟"),
            createMessage("Buenas noches 🌈 Cierra los ojos visualizando tu mejor versión", MessageCategory.EVENING, MessageTone.INSPIRATIONAL, "🌈"),
            createMessage("Es hora de soñar ✨ Tus sueños son el mapa de tu destino", MessageCategory.EVENING, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Buenas noches 🦋 Mañana despertarás más cerca de tus sueños", MessageCategory.EVENING, MessageTone.INSPIRATIONAL, "🦋"),
            createMessage("Descansa 🌻 Grandes cosas te esperan al amanecer", MessageCategory.EVENING, MessageTone.INSPIRATIONAL, "🌻"),
            
            createMessage("Buenas noches 📋 Revisa tus logros del día antes de dormir", MessageCategory.EVENING, MessageTone.PRACTICAL, "📋"),
            createMessage("Es hora de dormir 📵 Deja el celular 30 min antes para mejor sueño", MessageCategory.EVENING, MessageTone.PRACTICAL, "📵"),
            createMessage("Buenas noches 📝 Anota 3 cosas por las que agradecer hoy", MessageCategory.EVENING, MessageTone.PRACTICAL, "📝"),
            createMessage("Hora de descansar 🌡️ Temperatura ideal para dormir: 18-20°C", MessageCategory.EVENING, MessageTone.PRACTICAL, "🌡️"),
            createMessage("Buenas noches 💧 Un vaso de agua antes de dormir ayuda a tu cuerpo", MessageCategory.EVENING, MessageTone.PRACTICAL, "💧"),
            createMessage("Es hora 🧘 5 minutos de respiración profunda = mejor calidad de sueño", MessageCategory.EVENING, MessageTone.PRACTICAL, "🧘")
        ))
        
        // ============== ACHIEVEMENT MESSAGES (60+) ==============
        messages.addAll(listOf(
            createMessage("¡Increíble! 🎉 Has completado otra meta. Tu constancia es tu superpoder", MessageCategory.ACHIEVEMENT, MessageTone.FRIENDLY, "🎉"),
            createMessage("¡Lo lograste! 🏆 Eres prueba de que el esfuerzo vale la pena", MessageCategory.ACHIEVEMENT, MessageTone.FRIENDLY, "🏆"),
            createMessage("¡Felicidades! 🌟 Cada logro te acerca más a tu mejor versión", MessageCategory.ACHIEVEMENT, MessageTone.FRIENDLY, "🌟"),
            createMessage("¡Wow! ⭐ ¡Otro objetivo cumplido! Eres imparable", MessageCategory.ACHIEVEMENT, MessageTone.FRIENDLY, "⭐"),
            createMessage("¡Bravo! 👏 Tu dedicación está dando frutos increíbles", MessageCategory.ACHIEVEMENT, MessageTone.FRIENDLY, "👏"),
            createMessage("¡Excelente! 🎊 Celebra este logro, te lo mereces", MessageCategory.ACHIEVEMENT, MessageTone.FRIENDLY, "🎊"),
            createMessage("¡Genial! 💪 Otro paso adelante en tu camino al éxito", MessageCategory.ACHIEVEMENT, MessageTone.FRIENDLY, "💪"),
            createMessage("¡Asombroso! 🚀 Sigues superándote día tras día", MessageCategory.ACHIEVEMENT, MessageTone.FRIENDLY, "🚀"),
            
            createMessage("¡ESO ES! 💪 Esto es lo que pasa cuando no te rindes", MessageCategory.ACHIEVEMENT, MessageTone.COACH, "💪"),
            createMessage("¡VICTORIA! 🏆 Los campeones no se hacen, se forjan como tú", MessageCategory.ACHIEVEMENT, MessageTone.COACH, "🏆"),
            createMessage("¡Objetivo cumplido! 🎯 Ahora ve por el siguiente", MessageCategory.ACHIEVEMENT, MessageTone.COACH, "🎯"),
            createMessage("¡Lo hiciste! 🔥 Esta es la actitud de un ganador", MessageCategory.ACHIEVEMENT, MessageTone.COACH, "🔥"),
            createMessage("¡Boom! 💯 Demostaste que eres capaz de todo", MessageCategory.ACHIEVEMENT, MessageTone.COACH, "💯"),
            createMessage("¡Así se hace! 🏃 El trabajo duro SIEMPRE paga", MessageCategory.ACHIEVEMENT, MessageTone.COACH, "🏃"),
            createMessage("¡Éxito! 🎖️ Ahora sube el nivel de tus objetivos", MessageCategory.ACHIEVEMENT, MessageTone.COACH, "🎖️"),
            createMessage("¡Meta alcanzada! 💪 Recuerda esta sensación para los días difíciles", MessageCategory.ACHIEVEMENT, MessageTone.COACH, "💪"),
            
            createMessage("Lo lograste 🌟 Cada logro es una semilla de sabiduría plantada", MessageCategory.ACHIEVEMENT, MessageTone.WISE, "🌟"),
            createMessage("Felicidades 🧘 El éxito verdadero está en el camino, no solo en la meta", MessageCategory.ACHIEVEMENT, MessageTone.WISE, "🧘"),
            createMessage("Bien hecho 🌿 Has demostrado que la paciencia da frutos", MessageCategory.ACHIEVEMENT, MessageTone.WISE, "🌿"),
            createMessage("Lo conseguiste 🙏 La disciplina supera al talento cuando el talento no trabaja", MessageCategory.ACHIEVEMENT, MessageTone.WISE, "🙏"),
            createMessage("Victoria 🌸 En cada logro hay lecciones para el siguiente nivel", MessageCategory.ACHIEVEMENT, MessageTone.WISE, "🌸"),
            createMessage("Éxito 📿 Recuerda: el viaje importa tanto como el destino", MessageCategory.ACHIEVEMENT, MessageTone.WISE, "📿"),
            
            createMessage("¡¡¡YESSS!!! 🎉🎉🎉 ¡¡¡LO LOGRASTE!!! ¡¡¡ERES INCREÍBLE!!!", MessageCategory.ACHIEVEMENT, MessageTone.ENERGETIC, "🎉"),
            createMessage("¡¡¡BOOM!!! 💥💥💥 ¡¡¡OTRA META DESTRUIDA!!!", MessageCategory.ACHIEVEMENT, MessageTone.ENERGETIC, "💥"),
            createMessage("¡¡¡ÉPICO!!! 🔥🔥🔥 ¡¡¡NADA TE PUEDE DETENER!!!", MessageCategory.ACHIEVEMENT, MessageTone.ENERGETIC, "🔥"),
            createMessage("¡¡¡CAMPEÓN!!! 🏆🏆🏆 ¡¡¡ASÍ SE HACE HISTORIA!!!", MessageCategory.ACHIEVEMENT, MessageTone.ENERGETIC, "🏆"),
            createMessage("¡¡¡VICTORIA!!! ⚡⚡⚡ ¡¡¡ERES IMPARABLE!!!", MessageCategory.ACHIEVEMENT, MessageTone.ENERGETIC, "⚡"),
            createMessage("¡¡¡WOW!!! 🚀🚀🚀 ¡¡¡OTRO NIVEL DESBLOQUEADO!!!", MessageCategory.ACHIEVEMENT, MessageTone.ENERGETIC, "🚀"),
            
            createMessage("Lo lograste 🌸 Tómate un momento para apreciar tu esfuerzo", MessageCategory.ACHIEVEMENT, MessageTone.CALM, "🌸"),
            createMessage("Felicidades 🌿 Respira y celebra este momento de paz interior", MessageCategory.ACHIEVEMENT, MessageTone.CALM, "🌿"),
            createMessage("Bien hecho 🌊 Como las olas, llegaste a la orilla de tu meta", MessageCategory.ACHIEVEMENT, MessageTone.CALM, "🌊"),
            createMessage("Éxito 🕊️ Que la satisfacción de este logro llene tu corazón", MessageCategory.ACHIEVEMENT, MessageTone.CALM, "🕊️"),
            createMessage("Lo conseguiste 🌷 Disfruta la calma que viene después del esfuerzo", MessageCategory.ACHIEVEMENT, MessageTone.CALM, "🌷"),
            
            createMessage("¡Lo lograste! 😂 Seguro tus excusas están tristes ahora", MessageCategory.ACHIEVEMENT, MessageTone.HUMOROUS, "😂"),
            createMessage("¡Felicidades! 🎉 Otro logro para presumir en las reuniones familiares", MessageCategory.ACHIEVEMENT, MessageTone.HUMOROUS, "🎉"),
            createMessage("¡Éxito! 🏆 Tu yo del pasado estaría muy orgulloso (y sorprendido)", MessageCategory.ACHIEVEMENT, MessageTone.HUMOROUS, "🏆"),
            createMessage("¡Bravo! 👏 Deberías ponerte una medalla... o un taco, tú decides", MessageCategory.ACHIEVEMENT, MessageTone.HUMOROUS, "👏"),
            createMessage("¡Lo hiciste! 🌟 Ahora puedes decir 'yo siempre supe que podía'", MessageCategory.ACHIEVEMENT, MessageTone.HUMOROUS, "🌟"),
            
            createMessage("¡Lo lograste! ✨ Este logro es solo el comienzo de cosas más grandes", MessageCategory.ACHIEVEMENT, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("¡Felicidades! 🌈 Eres la prueba viviente de que los sueños se cumplen", MessageCategory.ACHIEVEMENT, MessageTone.INSPIRATIONAL, "🌈"),
            createMessage("¡Increíble! 💫 El universo celebra cada paso que das hacia tu destino", MessageCategory.ACHIEVEMENT, MessageTone.INSPIRATIONAL, "💫"),
            createMessage("¡Victoria! 🦋 Tu transformación inspira a quienes te rodean", MessageCategory.ACHIEVEMENT, MessageTone.INSPIRATIONAL, "🦋"),
            createMessage("¡Éxito! 🌻 Cada logro tuyo ilumina el camino de otros", MessageCategory.ACHIEVEMENT, MessageTone.INSPIRATIONAL, "🌻"),
            
            createMessage("Meta cumplida 📊 Documenta qué funcionó para replicarlo", MessageCategory.ACHIEVEMENT, MessageTone.PRACTICAL, "📊"),
            createMessage("Logro alcanzado 📋 Ahora define tu siguiente objetivo SMART", MessageCategory.ACHIEVEMENT, MessageTone.PRACTICAL, "📋"),
            createMessage("Éxito 📝 Anota las lecciones aprendidas para futuros desafíos", MessageCategory.ACHIEVEMENT, MessageTone.PRACTICAL, "📝"),
            createMessage("Bien hecho 🎯 Revisa: ¿Qué harías diferente la próxima vez?", MessageCategory.ACHIEVEMENT, MessageTone.PRACTICAL, "🎯"),
            createMessage("Victoria 📈 Celebra, pero ya planifica el siguiente paso", MessageCategory.ACHIEVEMENT, MessageTone.PRACTICAL, "📈")
        ))
        
        // ============== FOCUS MESSAGES (50+) ==============
        messages.addAll(listOf(
            createMessage("Respira profundo 🧘‍♀️ Concéntrate en lo que puedes controlar ahora", MessageCategory.FOCUS, MessageTone.FRIENDLY, "🧘‍♀️"),
            createMessage("¡Enfócate! 🎯 Una cosa a la vez, paso a paso", MessageCategory.FOCUS, MessageTone.FRIENDLY, "🎯"),
            createMessage("Momento de concentración 🧠 Tú tienes el control de tu atención", MessageCategory.FOCUS, MessageTone.FRIENDLY, "🧠"),
            createMessage("Focus time 💪 Elimina distracciones y da lo mejor de ti", MessageCategory.FOCUS, MessageTone.FRIENDLY, "💪"),
            createMessage("¡Concentración! ⚡ Tu mente es más poderosa de lo que crees", MessageCategory.FOCUS, MessageTone.FRIENDLY, "⚡"),
            createMessage("Momento zen 🌿 Enfócate en el ahora, el resto puede esperar", MessageCategory.FOCUS, MessageTone.FRIENDLY, "🌿"),
            createMessage("¡Vamos! 🚀 Es hora de entrar en modo productivo", MessageCategory.FOCUS, MessageTone.FRIENDLY, "🚀"),
            createMessage("Focus mode ON 🔥 Nada te puede distraer cuando decides enfocarte", MessageCategory.FOCUS, MessageTone.FRIENDLY, "🔥"),
            
            createMessage("¡ENFÓCATE! 💪 Las distracciones son el enemigo del éxito", MessageCategory.FOCUS, MessageTone.COACH, "💪"),
            createMessage("¡Concentración máxima! 🎯 Los resultados requieren atención plena", MessageCategory.FOCUS, MessageTone.COACH, "🎯"),
            createMessage("¡Sin excusas! 🔥 Una hora de enfoque vale más que 8 de distracción", MessageCategory.FOCUS, MessageTone.COACH, "🔥"),
            createMessage("¡Ahora! 💯 El celular puede esperar, tus metas no", MessageCategory.FOCUS, MessageTone.COACH, "💯"),
            createMessage("¡Focus! 🏆 Los campeones dominan su atención", MessageCategory.FOCUS, MessageTone.COACH, "🏆"),
            createMessage("¡Bloquea distracciones! 📵 El éxito no viene scrolleando redes", MessageCategory.FOCUS, MessageTone.COACH, "📵"),
            createMessage("¡Concentración! 🏃 Tu competencia está enfocada, ¿y tú?", MessageCategory.FOCUS, MessageTone.COACH, "🏃"),
            
            createMessage("Enfócate 🧘 La mente dispersa no alcanza ningún destino", MessageCategory.FOCUS, MessageTone.WISE, "🧘"),
            createMessage("Concentración 🌿 En la quietud de la atención plena, surge la claridad", MessageCategory.FOCUS, MessageTone.WISE, "🌿"),
            createMessage("Momento presente 🙏 Solo existe ahora, y ahora es tu momento de brillar", MessageCategory.FOCUS, MessageTone.WISE, "🙏"),
            createMessage("Focus 📿 La maestría nace de la práctica enfocada y consciente", MessageCategory.FOCUS, MessageTone.WISE, "📿"),
            createMessage("Atención plena 🌸 Donde pones tu enfoque, crece tu vida", MessageCategory.FOCUS, MessageTone.WISE, "🌸"),
            createMessage("Concentración 🕯️ Una vela enfocada ilumina más que mil dispersas", MessageCategory.FOCUS, MessageTone.WISE, "🕯️"),
            
            createMessage("¡¡¡FOCUS MODE!!! ⚡⚡⚡ ¡¡¡ACTIVA TU PODER MENTAL!!!", MessageCategory.FOCUS, MessageTone.ENERGETIC, "⚡"),
            createMessage("¡¡¡CONCENTRACIÓN!!! 🔥🔥🔥 ¡¡¡NADA TE DISTRAE!!!", MessageCategory.FOCUS, MessageTone.ENERGETIC, "🔥"),
            createMessage("¡¡¡VAMOS!!! 💪💪💪 ¡¡¡ENFÓCATE Y DESTRUYE ESA TAREA!!!", MessageCategory.FOCUS, MessageTone.ENERGETIC, "💪"),
            createMessage("¡¡¡AHORA!!! 🚀🚀🚀 ¡¡¡ES TU MOMENTO DE BRILLAR!!!", MessageCategory.FOCUS, MessageTone.ENERGETIC, "🚀"),
            
            createMessage("Respira 🌊 Suelta la ansiedad y enfócate en este momento", MessageCategory.FOCUS, MessageTone.CALM, "🌊"),
            createMessage("Calma 🌿 En la tranquilidad encuentras la concentración más profunda", MessageCategory.FOCUS, MessageTone.CALM, "🌿"),
            createMessage("Paz interior 🌸 Desde el silencio mental surge el enfoque verdadero", MessageCategory.FOCUS, MessageTone.CALM, "🌸"),
            createMessage("Serenidad 🕊️ Enfócate suavemente, sin forzar, solo fluyendo", MessageCategory.FOCUS, MessageTone.CALM, "🕊️"),
            createMessage("Quietud 🍃 En la calma de tu mente, las respuestas aparecen", MessageCategory.FOCUS, MessageTone.CALM, "🍃"),
            
            createMessage("Enfócate 😅 Las redes sociales son como los ex: no valen tu tiempo ahora", MessageCategory.FOCUS, MessageTone.HUMOROUS, "😅"),
            createMessage("¡Concentración! 📱 Tu celular no te extraña tanto como crees", MessageCategory.FOCUS, MessageTone.HUMOROUS, "📱"),
            createMessage("Focus time 🧠 Tu cerebro tiene tabs infinitos, cierra algunos", MessageCategory.FOCUS, MessageTone.HUMOROUS, "🧠"),
            createMessage("¡Enfócate! 🐿️ Las distracciones son como ardillas: ignóralas", MessageCategory.FOCUS, MessageTone.HUMOROUS, "🐿️"),
            
            createMessage("Enfócate ✨ Dentro de ti hay un poder ilimitado esperando ser liberado", MessageCategory.FOCUS, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Concentración 🌟 Tu mente enfocada puede mover montañas", MessageCategory.FOCUS, MessageTone.INSPIRATIONAL, "🌟"),
            createMessage("Focus 💫 Cuando te concentras, te conectas con tu verdadero potencial", MessageCategory.FOCUS, MessageTone.INSPIRATIONAL, "💫"),
            createMessage("Atención plena 🦋 Cada momento de enfoque te transforma", MessageCategory.FOCUS, MessageTone.INSPIRATIONAL, "🦋"),
            
            createMessage("Técnica Pomodoro 🍅 25 min de enfoque + 5 de descanso = productividad máxima", MessageCategory.FOCUS, MessageTone.PRACTICAL, "🍅"),
            createMessage("Bloqueo de tiempo ⏰ Programa 2h sin interrupciones para tareas importantes", MessageCategory.FOCUS, MessageTone.PRACTICAL, "⏰"),
            createMessage("Ambiente óptimo 🎧 Música lo-fi o silencio, elige lo que funcione para ti", MessageCategory.FOCUS, MessageTone.PRACTICAL, "🎧"),
            createMessage("Deep work 📵 Modo avión + lugar tranquilo = concentración garantizada", MessageCategory.FOCUS, MessageTone.PRACTICAL, "📵"),
            createMessage("Single-tasking 🎯 Una tarea a la vez, multitasking es un mito", MessageCategory.FOCUS, MessageTone.PRACTICAL, "🎯")
        ))
        
        // ============== GRATITUDE MESSAGES (50+) ==============
        messages.addAll(listOf(
            createMessage("Tómate un momento para apreciar lo lejos que has llegado 💚", MessageCategory.GRATITUDE, MessageTone.FRIENDLY, "💚"),
            createMessage("Gracias por ser tú 🌟 El mundo es mejor porque existes", MessageCategory.GRATITUDE, MessageTone.FRIENDLY, "🌟"),
            createMessage("Mira a tu alrededor 🌈 Hay tanto por lo que estar agradecido", MessageCategory.GRATITUDE, MessageTone.FRIENDLY, "🌈"),
            createMessage("Agradece este momento 🌸 Es un regalo que no se repite", MessageCategory.GRATITUDE, MessageTone.FRIENDLY, "🌸"),
            createMessage("Tu vida es valiosa 💎 Cada día es una oportunidad de agradecer", MessageCategory.GRATITUDE, MessageTone.FRIENDLY, "💎"),
            createMessage("Sonríe 😊 Tienes más bendiciones de las que puedes contar", MessageCategory.GRATITUDE, MessageTone.FRIENDLY, "😊"),
            createMessage("Celebra las pequeñas cosas 🎉 La gratitud vive en los detalles", MessageCategory.GRATITUDE, MessageTone.FRIENDLY, "🎉"),
            createMessage("Eres afortunado 🍀 Por respirar, por soñar, por existir", MessageCategory.GRATITUDE, MessageTone.FRIENDLY, "🍀"),
            
            createMessage("La gratitud es combustible 💪 Los ganadores agradecen incluso las derrotas", MessageCategory.GRATITUDE, MessageTone.COACH, "💪"),
            createMessage("Agradece el proceso 🎯 Cada obstáculo te hace más fuerte", MessageCategory.GRATITUDE, MessageTone.COACH, "🎯"),
            createMessage("Valora tu progreso 🏆 Mira dónde empezaste vs dónde estás", MessageCategory.GRATITUDE, MessageTone.COACH, "🏆"),
            createMessage("Gratitud = Fortaleza 🔥 Los más fuertes saben agradecer", MessageCategory.GRATITUDE, MessageTone.COACH, "🔥"),
            
            createMessage("La gratitud transforma la perspectiva 🧘 Lo que tienes es suficiente", MessageCategory.GRATITUDE, MessageTone.WISE, "🧘"),
            createMessage("Agradecer es reconocer 🙏 Que la vida te bendice constantemente", MessageCategory.GRATITUDE, MessageTone.WISE, "🙏"),
            createMessage("En cada respiración hay un regalo 🌿 Agradécelo conscientemente", MessageCategory.GRATITUDE, MessageTone.WISE, "🌿"),
            createMessage("La abundancia comienza con la gratitud 📿 Lo que aprecias, crece", MessageCategory.GRATITUDE, MessageTone.WISE, "📿"),
            createMessage("Gratitud es sabiduría 🕯️ Reconocer lo bueno en todo momento", MessageCategory.GRATITUDE, MessageTone.WISE, "🕯️"),
            createMessage("Agradecer el presente 🌸 Es la llave para un futuro abundante", MessageCategory.GRATITUDE, MessageTone.WISE, "🌸"),
            
            createMessage("Gratitud 🌊 Deja que la calma de agradecer llene tu corazón", MessageCategory.GRATITUDE, MessageTone.CALM, "🌊"),
            createMessage("Respira y agradece 🍃 Este momento es perfecto tal como es", MessageCategory.GRATITUDE, MessageTone.CALM, "🍃"),
            createMessage("Paz en la gratitud 🌷 Tu corazón se expande cuando agradeces", MessageCategory.GRATITUDE, MessageTone.CALM, "🌷"),
            createMessage("Serenidad 🕊️ La gratitud silenciosa es la más poderosa", MessageCategory.GRATITUDE, MessageTone.CALM, "🕊️"),
            createMessage("Calma agradecida 🌺 En la quietud, reconoces todas tus bendiciones", MessageCategory.GRATITUDE, MessageTone.CALM, "🌺"),
            
            createMessage("Agradece 😄 Que al menos no eres un cactus... las cactus no pueden comer tacos", MessageCategory.GRATITUDE, MessageTone.HUMOROUS, "😄"),
            createMessage("Gratitud 🙏 Por el WiFi, el café, y este mensaje motivacional", MessageCategory.GRATITUDE, MessageTone.HUMOROUS, "🙏"),
            createMessage("Da gracias 🌟 Por no ser un lunes permanente", MessageCategory.GRATITUDE, MessageTone.HUMOROUS, "🌟"),
            createMessage("Agradece 😂 Que tu yo del pasado no tomó peores decisiones", MessageCategory.GRATITUDE, MessageTone.HUMOROUS, "😂"),
            
            createMessage("La gratitud eleva tu vibración ✨ Agradece y atrae más bendiciones", MessageCategory.GRATITUDE, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Agradecer es crear 💫 Cada gracias abre puertas invisibles", MessageCategory.GRATITUDE, MessageTone.INSPIRATIONAL, "💫"),
            createMessage("Tu corazón agradecido 🌈 Tiene el poder de transformar tu realidad", MessageCategory.GRATITUDE, MessageTone.INSPIRATIONAL, "🌈"),
            createMessage("La gratitud es magia 🌟 Convierte lo ordinario en extraordinario", MessageCategory.GRATITUDE, MessageTone.INSPIRATIONAL, "🌟"),
            createMessage("Agradece y crece 🦋 Cada gracias te acerca a tu mejor versión", MessageCategory.GRATITUDE, MessageTone.INSPIRATIONAL, "🦋"),
            
            createMessage("Práctica de gratitud 📝 Escribe 3 cosas por las que agradeces hoy", MessageCategory.GRATITUDE, MessageTone.PRACTICAL, "📝"),
            createMessage("Gratitud activa 🎯 Envía un mensaje de agradecimiento a alguien", MessageCategory.GRATITUDE, MessageTone.PRACTICAL, "🎯"),
            createMessage("Diario de gratitud 📓 5 minutos diarios cambian tu perspectiva", MessageCategory.GRATITUDE, MessageTone.PRACTICAL, "📓"),
            createMessage("Gratitud consciente 🧘 Antes de dormir, recuerda 3 momentos buenos del día", MessageCategory.GRATITUDE, MessageTone.PRACTICAL, "🧘")
        ))
        
        // ============== CHALLENGE MESSAGES (40+) ==============
        messages.addAll(listOf(
            createMessage("Los desafíos te hacen más fuerte 💪 No te rindas ahora", MessageCategory.CHALLENGE, MessageTone.FRIENDLY, "💪"),
            createMessage("Cada obstáculo es una oportunidad disfrazada 🌟 Sigue adelante", MessageCategory.CHALLENGE, MessageTone.FRIENDLY, "🌟"),
            createMessage("Tú puedes con esto 🦁 Has superado cosas peores antes", MessageCategory.CHALLENGE, MessageTone.FRIENDLY, "🦁"),
            createMessage("Los momentos difíciles forjan el carácter 🔥 Ánimo campeón", MessageCategory.CHALLENGE, MessageTone.FRIENDLY, "🔥"),
            createMessage("No te detengas ahora 🚀 Estás más cerca de lo que crees", MessageCategory.CHALLENGE, MessageTone.FRIENDLY, "🚀"),
            createMessage("Las tormentas pasan 🌈 Y tú seguirás brillando", MessageCategory.CHALLENGE, MessageTone.FRIENDLY, "🌈"),
            
            createMessage("¡SIN EXCUSAS! 💪 Los desafíos son escaleras hacia el éxito", MessageCategory.CHALLENGE, MessageTone.COACH, "💪"),
            createMessage("¡LEVÁNTATE! 🔥 Los campeones no se quedan en el suelo", MessageCategory.CHALLENGE, MessageTone.COACH, "🔥"),
            createMessage("¡Más fuerte! 🏆 Lo que no te mata te hace invencible", MessageCategory.CHALLENGE, MessageTone.COACH, "🏆"),
            createMessage("¡AHORA! 💯 Es cuando demuestras de qué estás hecho", MessageCategory.CHALLENGE, MessageTone.COACH, "💯"),
            createMessage("¡No cedas! 🎯 La victoria está al otro lado de la dificultad", MessageCategory.CHALLENGE, MessageTone.COACH, "🎯"),
            createMessage("¡Empuja! 🏃 El dolor es temporal, el orgullo es para siempre", MessageCategory.CHALLENGE, MessageTone.COACH, "🏃"),
            
            createMessage("Los desafíos son maestros 🧘 Cada uno trae una lección valiosa", MessageCategory.CHALLENGE, MessageTone.WISE, "🧘"),
            createMessage("En la adversidad nacemos de nuevo 🌿 Más fuertes y sabios", MessageCategory.CHALLENGE, MessageTone.WISE, "🌿"),
            createMessage("El diamante se forma bajo presión 💎 Tú también", MessageCategory.CHALLENGE, MessageTone.WISE, "💎"),
            createMessage("La resistencia construye resiliencia 🙏 Confía en el proceso", MessageCategory.CHALLENGE, MessageTone.WISE, "🙏"),
            createMessage("El obstáculo es el camino 📿 Abraza el desafío como guía", MessageCategory.CHALLENGE, MessageTone.WISE, "📿"),
            
            createMessage("¡¡¡VAMOS!!! 💪💪💪 ¡¡¡LOS OBSTÁCULOS SON TU COMBUSTIBLE!!!", MessageCategory.CHALLENGE, MessageTone.ENERGETIC, "💪"),
            createMessage("¡¡¡IMPARABLE!!! 🔥🔥🔥 ¡¡¡NADA TE DETIENE!!!", MessageCategory.CHALLENGE, MessageTone.ENERGETIC, "🔥"),
            createMessage("¡¡¡GUERRERO!!! ⚔️⚔️⚔️ ¡¡¡NACISTE PARA SUPERAR ESTO!!!", MessageCategory.CHALLENGE, MessageTone.ENERGETIC, "⚔️"),
            
            createMessage("Respira 🌊 Este momento difícil también pasará", MessageCategory.CHALLENGE, MessageTone.CALM, "🌊"),
            createMessage("Calma 🍃 En la serenidad encuentras la fuerza para continuar", MessageCategory.CHALLENGE, MessageTone.CALM, "🍃"),
            createMessage("Paz interior 🌸 Los desafíos externos no tocan tu centro", MessageCategory.CHALLENGE, MessageTone.CALM, "🌸"),
            
            createMessage("Los desafíos son como gimnasio 💪 Duelen pero te ponen fit", MessageCategory.CHALLENGE, MessageTone.HUMOROUS, "💪"),
            createMessage("Si fuera fácil 😅 Cualquiera lo haría. Tú no eres cualquiera", MessageCategory.CHALLENGE, MessageTone.HUMOROUS, "😅"),
            createMessage("Los problemas son como WiFi malo 📶 Molestos pero se arreglan", MessageCategory.CHALLENGE, MessageTone.HUMOROUS, "📶"),
            
            createMessage("Eres más fuerte que cualquier desafío ✨ Brilla en la oscuridad", MessageCategory.CHALLENGE, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("El universo solo da batallas a guerreros 🌟 Tú fuiste elegido", MessageCategory.CHALLENGE, MessageTone.INSPIRATIONAL, "🌟"),
            createMessage("De las cenizas nace el fénix 🔥 Tu renacimiento está cerca", MessageCategory.CHALLENGE, MessageTone.INSPIRATIONAL, "🔥"),
            
            createMessage("Divide el problema 📋 Pasos pequeños = progreso grande", MessageCategory.CHALLENGE, MessageTone.PRACTICAL, "📋"),
            createMessage("Pide ayuda 🤝 Los fuertes saben cuándo necesitan apoyo", MessageCategory.CHALLENGE, MessageTone.PRACTICAL, "🤝"),
            createMessage("Analiza 🎯 ¿Qué puedes controlar? Enfócate solo en eso", MessageCategory.CHALLENGE, MessageTone.PRACTICAL, "🎯")
        ))
        
        // ============== STREAK MESSAGES (40+) ==============
        messages.addAll(listOf(
            createMessage("¡Tu racha sigue viva! 🔥 Otro día de constancia increíble", MessageCategory.STREAK, MessageTone.FRIENDLY, "🔥"),
            createMessage("¡Día tras día! 💪 Tu consistencia es admirable", MessageCategory.STREAK, MessageTone.FRIENDLY, "💪"),
            createMessage("¡Sigue así! 🌟 Cada día cuenta en tu racha de éxito", MessageCategory.STREAK, MessageTone.FRIENDLY, "🌟"),
            createMessage("¡Imparable! 🚀 Tu racha demuestra tu compromiso", MessageCategory.STREAK, MessageTone.FRIENDLY, "🚀"),
            createMessage("¡No rompas la cadena! ⛓️ Tu esfuerzo vale oro", MessageCategory.STREAK, MessageTone.FRIENDLY, "⛓️"),
            createMessage("¡Racha en fuego! 🔥🔥🔥 Eres una máquina de disciplina", MessageCategory.STREAK, MessageTone.FRIENDLY, "🔥"),
            
            createMessage("¡RACHA ACTIVA! 💪 Los campeones no fallan un solo día", MessageCategory.STREAK, MessageTone.COACH, "💪"),
            createMessage("¡Mantén el ritmo! 🏆 La consistencia supera al talento", MessageCategory.STREAK, MessageTone.COACH, "🏆"),
            createMessage("¡No cedas! 🔥 Un día de pereza destruye semanas de trabajo", MessageCategory.STREAK, MessageTone.COACH, "🔥"),
            createMessage("¡Disciplina! 💯 Tu racha es prueba de tu mentalidad ganadora", MessageCategory.STREAK, MessageTone.COACH, "💯"),
            createMessage("¡Sin parar! 🎯 Cada día alimenta tu racha de victoria", MessageCategory.STREAK, MessageTone.COACH, "🎯"),
            
            createMessage("La consistencia es el secreto 🧘 Pequeños actos diarios crean grandes cambios", MessageCategory.STREAK, MessageTone.WISE, "🧘"),
            createMessage("Día a día 🌿 La racha es el camino, no el destino", MessageCategory.STREAK, MessageTone.WISE, "🌿"),
            createMessage("Constancia 🙏 El agua perfora la piedra no por fuerza, sino por persistencia", MessageCategory.STREAK, MessageTone.WISE, "🙏"),
            createMessage("Tu racha refleja 📿 El poder de la repetición consciente", MessageCategory.STREAK, MessageTone.WISE, "📿"),
            
            createMessage("¡¡¡RACHA ÉPICA!!! 🔥🔥🔥 ¡¡¡ERES UNA LEYENDA!!!", MessageCategory.STREAK, MessageTone.ENERGETIC, "🔥"),
            createMessage("¡¡¡IMPARABLE!!! 💪💪💪 ¡¡¡TU RACHA ES LEGENDARIA!!!", MessageCategory.STREAK, MessageTone.ENERGETIC, "💪"),
            createMessage("¡¡¡BOOM!!! ⚡⚡⚡ ¡¡¡OTRO DÍA DOMINADO!!!", MessageCategory.STREAK, MessageTone.ENERGETIC, "⚡"),
            
            createMessage("Tu racha continúa 🌸 Celebra cada día de constancia con calma", MessageCategory.STREAK, MessageTone.CALM, "🌸"),
            createMessage("Día tras día 🌊 Fluyes con la disciplina que has cultivado", MessageCategory.STREAK, MessageTone.CALM, "🌊"),
            createMessage("Consistencia serena 🍃 No hay prisa, solo presencia diaria", MessageCategory.STREAK, MessageTone.CALM, "🍃"),
            
            createMessage("Tu racha 😂 Es más larga que algunas de mis relaciones", MessageCategory.STREAK, MessageTone.HUMOROUS, "😂"),
            createMessage("¡Racha activa! 🔥 Si fuera pizza, ya tendrías delivery gratis", MessageCategory.STREAK, MessageTone.HUMOROUS, "🔥"),
            createMessage("Días consecutivos 📆 Netflix debería aprender de tu compromiso", MessageCategory.STREAK, MessageTone.HUMOROUS, "📆"),
            
            createMessage("Tu racha es inspiración ✨ Cada día escribes tu leyenda", MessageCategory.STREAK, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Constancia mágica 🌟 Tu disciplina crea milagros diarios", MessageCategory.STREAK, MessageTone.INSPIRATIONAL, "🌟"),
            createMessage("Racha de luz 💫 Iluminas tu camino día tras día", MessageCategory.STREAK, MessageTone.INSPIRATIONAL, "💫"),
            
            createMessage("Protege tu racha 📋 Planifica tu día para no fallar", MessageCategory.STREAK, MessageTone.PRACTICAL, "📋"),
            createMessage("Racha = Hábito 🧠 21 días crean el patrón, 66 lo solidifican", MessageCategory.STREAK, MessageTone.PRACTICAL, "🧠"),
            createMessage("Recordatorio 🎯 Hazlo temprano para asegurar otro día de racha", MessageCategory.STREAK, MessageTone.PRACTICAL, "🎯")
        ))
        
        // ============== CELEBRATION MESSAGES (40+) ==============
        messages.addAll(listOf(
            createMessage("¡Es momento de celebrar! 🎉 Has trabajado duro para esto", MessageCategory.CELEBRATION, MessageTone.FRIENDLY, "🎉"),
            createMessage("¡Fiesta! 🎊 Otro logro que merece reconocimiento", MessageCategory.CELEBRATION, MessageTone.FRIENDLY, "🎊"),
            createMessage("¡Celebra! 🥳 Cada victoria, grande o pequeña, cuenta", MessageCategory.CELEBRATION, MessageTone.FRIENDLY, "🥳"),
            createMessage("¡Bravo! 👏 Tómate un momento para disfrutar tu éxito", MessageCategory.CELEBRATION, MessageTone.FRIENDLY, "👏"),
            createMessage("¡Increíble! 🌟 Mereces este momento de alegría", MessageCategory.CELEBRATION, MessageTone.FRIENDLY, "🌟"),
            createMessage("¡Woohoo! 🎈 Celebra como el campeón que eres", MessageCategory.CELEBRATION, MessageTone.FRIENDLY, "🎈"),
            
            createMessage("¡VICTORIA! 🏆 Celebra y luego vuelve al trabajo, campeón", MessageCategory.CELEBRATION, MessageTone.COACH, "🏆"),
            createMessage("¡Lo lograste! 💪 Disfruta este momento, te lo ganaste", MessageCategory.CELEBRATION, MessageTone.COACH, "💪"),
            createMessage("¡Éxito! 🔥 Celebra pero no te duermas en los laureles", MessageCategory.CELEBRATION, MessageTone.COACH, "🔥"),
            
            createMessage("Celebra con gratitud 🙏 Cada logro es un regalo del universo", MessageCategory.CELEBRATION, MessageTone.WISE, "🙏"),
            createMessage("Disfruta el momento 🧘 La alegría consciente multiplica las bendiciones", MessageCategory.CELEBRATION, MessageTone.WISE, "🧘"),
            createMessage("Celebración serena 🌿 En la quietud, la alegría es más profunda", MessageCategory.CELEBRATION, MessageTone.WISE, "🌿"),
            
            createMessage("¡¡¡FIESTA!!! 🎉🎉🎉 ¡¡¡LO LOGRASTE!!! ¡¡¡CELEBRA A LO GRANDE!!!", MessageCategory.CELEBRATION, MessageTone.ENERGETIC, "🎉"),
            createMessage("¡¡¡WOOOOW!!! 🎊🎊🎊 ¡¡¡ERES INCREÍBLE!!! ¡¡¡FESTEJA!!!", MessageCategory.CELEBRATION, MessageTone.ENERGETIC, "🎊"),
            createMessage("¡¡¡ÉPICO!!! 🥳🥳🥳 ¡¡¡BAILA LA VICTORIA!!!", MessageCategory.CELEBRATION, MessageTone.ENERGETIC, "🥳"),
            
            createMessage("Celebra suavemente 🌸 La alegría tranquila es la más duradera", MessageCategory.CELEBRATION, MessageTone.CALM, "🌸"),
            createMessage("Sonríe 🌷 Deja que la satisfacción llene tu corazón en silencio", MessageCategory.CELEBRATION, MessageTone.CALM, "🌷"),
            createMessage("Paz victoriosa 🕊️ Celebra con serenidad y gratitud", MessageCategory.CELEBRATION, MessageTone.CALM, "🕊️"),
            
            createMessage("¡Celebra! 🎂 Aunque sea martes y no haya pastel", MessageCategory.CELEBRATION, MessageTone.HUMOROUS, "🎂"),
            createMessage("¡Fiesta! 🎉 Ok, quizás solo un snack, pero con actitud de fiesta", MessageCategory.CELEBRATION, MessageTone.HUMOROUS, "🎉"),
            createMessage("¡Woohoo! 🥳 Baila aunque los vecinos te vean raro", MessageCategory.CELEBRATION, MessageTone.HUMOROUS, "🥳"),
            
            createMessage("Tu victoria inspira ✨ Celebra sabiendo que iluminas el mundo", MessageCategory.CELEBRATION, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Celebración mágica 🌈 El universo festeja contigo", MessageCategory.CELEBRATION, MessageTone.INSPIRATIONAL, "🌈"),
            createMessage("Alegría radiante 🌟 Tu éxito eleva a todos a tu alrededor", MessageCategory.CELEBRATION, MessageTone.INSPIRATIONAL, "🌟"),
            
            createMessage("Celebra y documenta 📸 Estas memorias te motivarán después", MessageCategory.CELEBRATION, MessageTone.PRACTICAL, "📸"),
            createMessage("Recompensa merecida 🎁 Planifica algo especial para celebrar", MessageCategory.CELEBRATION, MessageTone.PRACTICAL, "🎁"),
            createMessage("Comparte tu éxito 📱 Celebrar con otros multiplica la alegría", MessageCategory.CELEBRATION, MessageTone.PRACTICAL, "📱")
        ))
        
        // ============== MOTIVATION GENERAL (50+) ==============
        messages.addAll(listOf(
            createMessage("Eres capaz de cosas increíbles ✨ Nunca lo dudes", MessageCategory.MOTIVATION, MessageTone.FRIENDLY, "✨"),
            createMessage("Hoy es un buen día para hacer algo grande 🌟", MessageCategory.MOTIVATION, MessageTone.FRIENDLY, "🌟"),
            createMessage("Cree en ti mismo 💪 Tienes todo lo que necesitas", MessageCategory.MOTIVATION, MessageTone.FRIENDLY, "💪"),
            createMessage("Tu potencial es ilimitado 🚀 Sigue adelante", MessageCategory.MOTIVATION, MessageTone.FRIENDLY, "🚀"),
            createMessage("Eres más fuerte de lo que crees 🦁 No te subestimes", MessageCategory.MOTIVATION, MessageTone.FRIENDLY, "🦁"),
            createMessage("El éxito te espera 🏆 Solo da el siguiente paso", MessageCategory.MOTIVATION, MessageTone.FRIENDLY, "🏆"),
            createMessage("Tu momento es ahora 🔥 Aprovéchalo al máximo", MessageCategory.MOTIVATION, MessageTone.FRIENDLY, "🔥"),
            createMessage("Eres imparable 💫 Cuando decides, lo logras", MessageCategory.MOTIVATION, MessageTone.FRIENDLY, "💫"),
            
            createMessage("¡Sin límites! 💪 Tu única competencia eres tú de ayer", MessageCategory.MOTIVATION, MessageTone.COACH, "💪"),
            createMessage("¡Hazlo! 🔥 Las excusas no construyen sueños", MessageCategory.MOTIVATION, MessageTone.COACH, "🔥"),
            createMessage("¡Ahora! 🎯 El momento perfecto es ahora mismo", MessageCategory.MOTIVATION, MessageTone.COACH, "🎯"),
            createMessage("¡Empuja! 💯 Los resultados están del otro lado del esfuerzo", MessageCategory.MOTIVATION, MessageTone.COACH, "💯"),
            createMessage("¡Adelante! 🏆 Los ganadores toman acción, no excusas", MessageCategory.MOTIVATION, MessageTone.COACH, "🏆"),
            createMessage("¡Muévete! 🏃 El éxito no viene a ti, tú vas hacia él", MessageCategory.MOTIVATION, MessageTone.COACH, "🏃"),
            
            createMessage("Tu camino es único 🧘 No te compares, solo avanza", MessageCategory.MOTIVATION, MessageTone.WISE, "🧘"),
            createMessage("La paciencia es poder 🌿 Los grandes logros toman tiempo", MessageCategory.MOTIVATION, MessageTone.WISE, "🌿"),
            createMessage("Confía en el proceso 🙏 Cada paso tiene propósito", MessageCategory.MOTIVATION, MessageTone.WISE, "🙏"),
            createMessage("El viaje es la recompensa 📿 Disfruta cada momento", MessageCategory.MOTIVATION, MessageTone.WISE, "📿"),
            createMessage("Eres suficiente 🌸 Tal como eres, en este momento", MessageCategory.MOTIVATION, MessageTone.WISE, "🌸"),
            createMessage("La vida te enseña 🕯️ Si estás dispuesto a aprender", MessageCategory.MOTIVATION, MessageTone.WISE, "🕯️"),
            
            createMessage("¡¡¡VAMOS!!! ⚡⚡⚡ ¡¡¡HOY ES TU DÍA!!!", MessageCategory.MOTIVATION, MessageTone.ENERGETIC, "⚡"),
            createMessage("¡¡¡PODER!!! 💪💪💪 ¡¡¡TIENES TODO PARA TRIUNFAR!!!", MessageCategory.MOTIVATION, MessageTone.ENERGETIC, "💪"),
            createMessage("¡¡¡ÉPICO!!! 🔥🔥🔥 ¡¡¡SÉ IMPARABLE HOY!!!", MessageCategory.MOTIVATION, MessageTone.ENERGETIC, "🔥"),
            createMessage("¡¡¡HAZLO!!! 🚀🚀🚀 ¡¡¡NO HAY LÍMITES PARA TI!!!", MessageCategory.MOTIVATION, MessageTone.ENERGETIC, "🚀"),
            
            createMessage("Respira 🌊 Estás exactamente donde necesitas estar", MessageCategory.MOTIVATION, MessageTone.CALM, "🌊"),
            createMessage("Todo está bien 🌿 Confía en tu camino", MessageCategory.MOTIVATION, MessageTone.CALM, "🌿"),
            createMessage("Paz interior 🌸 Es la base de todo logro duradero", MessageCategory.MOTIVATION, MessageTone.CALM, "🌸"),
            createMessage("Calma 🕊️ Desde la serenidad, todo es posible", MessageCategory.MOTIVATION, MessageTone.CALM, "🕊️"),
            
            createMessage("Eres genial 😎 Y si alguien no lo ve, necesitan lentes", MessageCategory.MOTIVATION, MessageTone.HUMOROUS, "😎"),
            createMessage("¡Tú puedes! 💪 Y si no puedes, finge hasta lograrlo", MessageCategory.MOTIVATION, MessageTone.HUMOROUS, "💪"),
            createMessage("Motivación 🎯 Es como el WiFi: invisible pero esencial", MessageCategory.MOTIVATION, MessageTone.HUMOROUS, "🎯"),
            createMessage("Ánimo 🌟 Peores cosas has superado (como esa dieta de enero)", MessageCategory.MOTIVATION, MessageTone.HUMOROUS, "🌟"),
            
            createMessage("Eres magia pura ✨ El universo celebra tu existencia", MessageCategory.MOTIVATION, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Tu luz interior 🌟 Ilumina caminos que ni imaginas", MessageCategory.MOTIVATION, MessageTone.INSPIRATIONAL, "🌟"),
            createMessage("Naciste para brillar 💫 No te conformes con menos", MessageCategory.MOTIVATION, MessageTone.INSPIRATIONAL, "💫"),
            createMessage("Eres extraordinario 🌈 Cada día es prueba de ello", MessageCategory.MOTIVATION, MessageTone.INSPIRATIONAL, "🌈"),
            createMessage("Tu potencial es infinito 🦋 Solo tú defines tus límites", MessageCategory.MOTIVATION, MessageTone.INSPIRATIONAL, "🦋"),
            
            createMessage("Acción > Motivación 🎯 Empieza y la motivación seguirá", MessageCategory.MOTIVATION, MessageTone.PRACTICAL, "🎯"),
            createMessage("Hábitos > Metas 📋 Construye sistemas, no solo objetivos", MessageCategory.MOTIVATION, MessageTone.PRACTICAL, "📋"),
            createMessage("1% mejor cada día 📈 = 37x mejor en un año", MessageCategory.MOTIVATION, MessageTone.PRACTICAL, "📈"),
            createMessage("Próximo paso 🦶 Enfócate solo en la siguiente acción", MessageCategory.MOTIVATION, MessageTone.PRACTICAL, "🦶")
        ))
        
        // ============== PRODUCTIVITY MESSAGES (40+) ==============
        messages.addAll(listOf(
            createMessage("Productividad = enfoque + energía 📈 Tienes ambas", MessageCategory.PRODUCTIVITY, MessageTone.FRIENDLY, "📈"),
            createMessage("Hoy serás ultra productivo 🚀 Lo siento en mis bytes", MessageCategory.PRODUCTIVITY, MessageTone.FRIENDLY, "🚀"),
            createMessage("Tu to-do list tiembla 📋 Porque hoy la vas a destruir", MessageCategory.PRODUCTIVITY, MessageTone.FRIENDLY, "📋"),
            createMessage("Modo productivo ON 💪 Nada te puede parar", MessageCategory.PRODUCTIVITY, MessageTone.FRIENDLY, "💪"),
            createMessage("Eres una máquina de productividad ⚙️ Eficiente y constante", MessageCategory.PRODUCTIVITY, MessageTone.FRIENDLY, "⚙️"),
            
            createMessage("¡A trabajar! 💪 El éxito no viene de pensar, sino de hacer", MessageCategory.PRODUCTIVITY, MessageTone.COACH, "💪"),
            createMessage("¡Productividad máxima! 🔥 Elimina distracciones y ejecuta", MessageCategory.PRODUCTIVITY, MessageTone.COACH, "🔥"),
            createMessage("¡Enfócate! 🎯 Una tarea terminada vale más que 10 empezadas", MessageCategory.PRODUCTIVITY, MessageTone.COACH, "🎯"),
            createMessage("¡Sin excusas! 💯 El trabajo duro supera al talento perezoso", MessageCategory.PRODUCTIVITY, MessageTone.COACH, "💯"),
            createMessage("¡Hazlo ahora! ⏰ La procrastinación es el ladrón del éxito", MessageCategory.PRODUCTIVITY, MessageTone.COACH, "⏰"),
            
            createMessage("La productividad verdadera 🧘 Nace de la claridad mental", MessageCategory.PRODUCTIVITY, MessageTone.WISE, "🧘"),
            createMessage("Menos es más 🌿 Enfócate en lo esencial", MessageCategory.PRODUCTIVITY, MessageTone.WISE, "🌿"),
            createMessage("Calidad sobre cantidad 🙏 Un trabajo excelente vale por mil mediocres", MessageCategory.PRODUCTIVITY, MessageTone.WISE, "🙏"),
            
            createMessage("¡¡¡PRODUCTIVIDAD!!! 💪💪💪 ¡¡¡DESTRUYE ESA LISTA DE TAREAS!!!", MessageCategory.PRODUCTIVITY, MessageTone.ENERGETIC, "💪"),
            createMessage("¡¡¡A TRABAJAR!!! 🔥🔥🔥 ¡¡¡MODO BESTIA ACTIVADO!!!", MessageCategory.PRODUCTIVITY, MessageTone.ENERGETIC, "🔥"),
            createMessage("¡¡¡VAMOS!!! ⚡⚡⚡ ¡¡¡HOY ERES IMPARABLE!!!", MessageCategory.PRODUCTIVITY, MessageTone.ENERGETIC, "⚡"),
            
            createMessage("Trabaja con calma 🌊 La productividad sostenible viene de la paz", MessageCategory.PRODUCTIVITY, MessageTone.CALM, "🌊"),
            createMessage("Fluye con el trabajo 🍃 Sin forzar, solo haciendo", MessageCategory.PRODUCTIVITY, MessageTone.CALM, "🍃"),
            
            createMessage("Productividad 😅 Es hacer todo menos lo que deberías", MessageCategory.PRODUCTIVITY, MessageTone.HUMOROUS, "😅"),
            createMessage("Tu lista de tareas 📋 Te mira con ojos tristes, dale amor", MessageCategory.PRODUCTIVITY, MessageTone.HUMOROUS, "📋"),
            createMessage("Procrastinar 🎯 Es el arte de mantenerse ocupado con lo menos importante", MessageCategory.PRODUCTIVITY, MessageTone.HUMOROUS, "🎯"),
            
            createMessage("Eres productividad pura ✨ Cuando decides, todo fluye", MessageCategory.PRODUCTIVITY, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Tu enfoque es poder 🌟 Transforma intención en resultados", MessageCategory.PRODUCTIVITY, MessageTone.INSPIRATIONAL, "🌟"),
            
            createMessage("Eat the frog 🐸 Haz la tarea más difícil primero", MessageCategory.PRODUCTIVITY, MessageTone.PRACTICAL, "🐸"),
            createMessage("Time blocking ⏰ Programa bloques de 2h para trabajo profundo", MessageCategory.PRODUCTIVITY, MessageTone.PRACTICAL, "⏰"),
            createMessage("2-Minute Rule ⚡ Si toma menos de 2 min, hazlo ahora", MessageCategory.PRODUCTIVITY, MessageTone.PRACTICAL, "⚡"),
            createMessage("Batch similar tasks 📦 Agrupa tareas similares para eficiencia", MessageCategory.PRODUCTIVITY, MessageTone.PRACTICAL, "📦"),
            createMessage("Prioriza con Eisenhower 📊 Urgente vs Importante, elige sabiamente", MessageCategory.PRODUCTIVITY, MessageTone.PRACTICAL, "📊")
        ))
        
        // ============== MINDFULNESS MESSAGES (40+) ==============
        messages.addAll(listOf(
            createMessage("Este momento es perfecto 🧠 Respira y disfrútalo", MessageCategory.MINDFULNESS, MessageTone.FRIENDLY, "🧠"),
            createMessage("Pausa y respira 🌬️ Tu bienestar es prioridad", MessageCategory.MINDFULNESS, MessageTone.FRIENDLY, "🌬️"),
            createMessage("Aquí y ahora 🌸 Es donde sucede la vida real", MessageCategory.MINDFULNESS, MessageTone.FRIENDLY, "🌸"),
            createMessage("Momento presente 💆 Suelta el pasado, olvida el futuro", MessageCategory.MINDFULNESS, MessageTone.FRIENDLY, "💆"),
            createMessage("Respira profundo 🧘 Tu cuerpo te lo agradecerá", MessageCategory.MINDFULNESS, MessageTone.FRIENDLY, "🧘"),
            
            createMessage("Mindfulness es fortaleza 💪 Los fuertes saben pausar", MessageCategory.MINDFULNESS, MessageTone.COACH, "💪"),
            createMessage("Momento de reset 🔄 Los atletas mentales meditan", MessageCategory.MINDFULNESS, MessageTone.COACH, "🔄"),
            
            createMessage("En el silencio encuentras 🧘 Las respuestas que buscas", MessageCategory.MINDFULNESS, MessageTone.WISE, "🧘"),
            createMessage("El presente es eterno 🌿 Todo lo demás es ilusión", MessageCategory.MINDFULNESS, MessageTone.WISE, "🌿"),
            createMessage("Mente tranquila 🙏 Ve más claro que mente agitada", MessageCategory.MINDFULNESS, MessageTone.WISE, "🙏"),
            createMessage("Observa sin juzgar 📿 Los pensamientos son nubes pasajeras", MessageCategory.MINDFULNESS, MessageTone.WISE, "📿"),
            createMessage("El ahora es todo 🌸 El pasado no existe, el futuro tampoco", MessageCategory.MINDFULNESS, MessageTone.WISE, "🌸"),
            createMessage("Paz interior 🕯️ Es independiente de las circunstancias externas", MessageCategory.MINDFULNESS, MessageTone.WISE, "🕯️"),
            
            createMessage("Respira profundo 🌊 Inhala calma, exhala tensión", MessageCategory.MINDFULNESS, MessageTone.CALM, "🌊"),
            createMessage("Este momento 🍃 Es un regalo que mereces disfrutar", MessageCategory.MINDFULNESS, MessageTone.CALM, "🍃"),
            createMessage("Suelta 🌷 Lo que no puedes controlar", MessageCategory.MINDFULNESS, MessageTone.CALM, "🌷"),
            createMessage("Aquí estás 🕊️ Presente, vivo, completo", MessageCategory.MINDFULNESS, MessageTone.CALM, "🕊️"),
            createMessage("Silencio interior 🌺 Es el espacio donde nace la claridad", MessageCategory.MINDFULNESS, MessageTone.CALM, "🌺"),
            createMessage("Respira 🌙 Y deja que todo fluya", MessageCategory.MINDFULNESS, MessageTone.CALM, "🌙"),
            
            createMessage("Mindfulness 😌 Es como WiFi para tu cerebro: conecta todo", MessageCategory.MINDFULNESS, MessageTone.HUMOROUS, "😌"),
            createMessage("Respira 🧘 Aunque sea para resistir no lanzar el celular", MessageCategory.MINDFULNESS, MessageTone.HUMOROUS, "🧘"),
            
            createMessage("Tu mente presente ✨ Es un portal a tu poder infinito", MessageCategory.MINDFULNESS, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("En el ahora 🌟 Vive tu verdadero ser", MessageCategory.MINDFULNESS, MessageTone.INSPIRATIONAL, "🌟"),
            createMessage("Mindfulness 💫 Es despertar a la magia del momento", MessageCategory.MINDFULNESS, MessageTone.INSPIRATIONAL, "💫"),
            
            createMessage("4-7-8 breathing 🌬️ Inhala 4s, sostén 7s, exhala 8s", MessageCategory.MINDFULNESS, MessageTone.PRACTICAL, "🌬️"),
            createMessage("Body scan 🧘 Recorre tu cuerpo notando sensaciones", MessageCategory.MINDFULNESS, MessageTone.PRACTICAL, "🧘"),
            createMessage("5 sentidos 👀 Nota 5 cosas que ves, 4 que oyes, 3 que sientes...", MessageCategory.MINDFULNESS, MessageTone.PRACTICAL, "👀"),
            createMessage("Micro-meditación ⏱️ 3 respiraciones conscientes = reset mental", MessageCategory.MINDFULNESS, MessageTone.PRACTICAL, "⏱️")
        ))
        
        // ============== SELF-CARE MESSAGES (35+) ==============
        messages.addAll(listOf(
            createMessage("Cuídate hoy ❤️ Eres tu recurso más valioso", MessageCategory.SELF_CARE, MessageTone.FRIENDLY, "❤️"),
            createMessage("Tu bienestar importa 💆 Date un momento para ti", MessageCategory.SELF_CARE, MessageTone.FRIENDLY, "💆"),
            createMessage("Mereces descanso 🌸 No eres una máquina", MessageCategory.SELF_CARE, MessageTone.FRIENDLY, "🌸"),
            createMessage("Ámate hoy 💕 Como amarías a tu mejor amigo", MessageCategory.SELF_CARE, MessageTone.FRIENDLY, "💕"),
            createMessage("Tu cuerpo te habla 👂 Escúchalo con amor", MessageCategory.SELF_CARE, MessageTone.FRIENDLY, "👂"),
            createMessage("Prioriza tu salud 🍎 Todo lo demás viene después", MessageCategory.SELF_CARE, MessageTone.FRIENDLY, "🍎"),
            
            createMessage("El autocuidado no es debilidad 💪 Es estrategia", MessageCategory.SELF_CARE, MessageTone.COACH, "💪"),
            createMessage("Los atletas descansan 🏆 Tú también debes hacerlo", MessageCategory.SELF_CARE, MessageTone.COACH, "🏆"),
            createMessage("Recuperación = Rendimiento 🔥 No te saltes el descanso", MessageCategory.SELF_CARE, MessageTone.COACH, "🔥"),
            
            createMessage("Cuidarte es un acto de sabiduría 🧘 No de egoísmo", MessageCategory.SELF_CARE, MessageTone.WISE, "🧘"),
            createMessage("Tu cuerpo es tu templo 🌿 Trátalo con reverencia", MessageCategory.SELF_CARE, MessageTone.WISE, "🌿"),
            createMessage("Amor propio 🙏 Es la base de todo amor verdadero", MessageCategory.SELF_CARE, MessageTone.WISE, "🙏"),
            
            createMessage("Cuídate 🌊 Mereces la misma compasión que das a otros", MessageCategory.SELF_CARE, MessageTone.CALM, "🌊"),
            createMessage("Descansa 🍃 Tu alma necesita momentos de quietud", MessageCategory.SELF_CARE, MessageTone.CALM, "🍃"),
            createMessage("Ámate suavemente 🌷 Con la ternura de mil abrazos", MessageCategory.SELF_CARE, MessageTone.CALM, "🌷"),
            
            createMessage("Autocuidado 😄 No es solo mascarillas, también es decir que no", MessageCategory.SELF_CARE, MessageTone.HUMOROUS, "😄"),
            createMessage("Cuídate 🛁 Tu bañera te extraña más que tu ex", MessageCategory.SELF_CARE, MessageTone.HUMOROUS, "🛁"),
            createMessage("Self-care 😌 Es permiso para ignorar mensajes mientras descansas", MessageCategory.SELF_CARE, MessageTone.HUMOROUS, "😌"),
            
            createMessage("Eres digno de amor ✨ Especialmente el tuyo propio", MessageCategory.SELF_CARE, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Tu bienestar 🌟 Eleva a todos los que te rodean", MessageCategory.SELF_CARE, MessageTone.INSPIRATIONAL, "🌟"),
            createMessage("Cuidarte 💫 Es honrar el regalo de tu existencia", MessageCategory.SELF_CARE, MessageTone.INSPIRATIONAL, "💫"),
            
            createMessage("Checklist de autocuidado 📋 ¿Agua? ¿Comida? ¿Descanso? ¿Movimiento?", MessageCategory.SELF_CARE, MessageTone.PRACTICAL, "📋"),
            createMessage("Sleep hygiene 😴 Misma hora de dormir, sin pantallas 30 min antes", MessageCategory.SELF_CARE, MessageTone.PRACTICAL, "😴"),
            createMessage("Hidratación 💧 8 vasos de agua al día mantienen al doctor lejos", MessageCategory.SELF_CARE, MessageTone.PRACTICAL, "💧"),
            createMessage("Micro-descansos 🔄 5 min cada hora = mayor productividad", MessageCategory.SELF_CARE, MessageTone.PRACTICAL, "🔄")
        ))
        
        // ============== GROWTH MESSAGES (35+) ==============
        messages.addAll(listOf(
            createMessage("Cada día creces un poco más 🌱 Sigue así", MessageCategory.GROWTH, MessageTone.FRIENDLY, "🌱"),
            createMessage("Tu evolución es constante 🦋 Aunque no siempre la notes", MessageCategory.GROWTH, MessageTone.FRIENDLY, "🦋"),
            createMessage("Eres work in progress 🔨 Y eso está perfecto", MessageCategory.GROWTH, MessageTone.FRIENDLY, "🔨"),
            createMessage("Crecimiento personal 📈 Es un maratón, no un sprint", MessageCategory.GROWTH, MessageTone.FRIENDLY, "📈"),
            createMessage("Cada error es aprendizaje 📚 Sigue experimentando", MessageCategory.GROWTH, MessageTone.FRIENDLY, "📚"),
            
            createMessage("¡Crece! 💪 La zona de confort es el cementerio de los sueños", MessageCategory.GROWTH, MessageTone.COACH, "💪"),
            createMessage("¡Expande! 🔥 Tu potencial no tiene límites si trabajas duro", MessageCategory.GROWTH, MessageTone.COACH, "🔥"),
            createMessage("¡Mejora! 🏆 1% mejor cada día = 37x mejor en un año", MessageCategory.GROWTH, MessageTone.COACH, "🏆"),
            createMessage("¡Desafíate! 🎯 El crecimiento está fuera de lo fácil", MessageCategory.GROWTH, MessageTone.COACH, "🎯"),
            
            createMessage("El crecimiento es inevitable 🧘 Si eliges conscientemente aprender", MessageCategory.GROWTH, MessageTone.WISE, "🧘"),
            createMessage("Cada experiencia te moldea 🌿 Para tu siguiente nivel", MessageCategory.GROWTH, MessageTone.WISE, "🌿"),
            createMessage("La vida es un maestro 🙏 Si eres un estudiante dispuesto", MessageCategory.GROWTH, MessageTone.WISE, "🙏"),
            createMessage("Creces como el bambú 🎋 Raíces profundas antes del brote visible", MessageCategory.GROWTH, MessageTone.WISE, "🎋"),
            
            createMessage("¡¡¡CRECE!!! 🌱🌱🌱 ¡¡¡ROMPE TUS LÍMITES!!!", MessageCategory.GROWTH, MessageTone.ENERGETIC, "🌱"),
            createMessage("¡¡¡EVOLUCIONA!!! 🦋🦋🦋 ¡¡¡TU MEJOR VERSIÓN TE ESPERA!!!", MessageCategory.GROWTH, MessageTone.ENERGETIC, "🦋"),
            
            createMessage("Crece con paciencia 🌊 Las mejores transformaciones toman tiempo", MessageCategory.GROWTH, MessageTone.CALM, "🌊"),
            createMessage("Evolución serena 🍃 Fluye con tu desarrollo natural", MessageCategory.GROWTH, MessageTone.CALM, "🍃"),
            
            createMessage("Crecimiento personal 😅 Es descubrir que el villano eras tú todo el tiempo", MessageCategory.GROWTH, MessageTone.HUMOROUS, "😅"),
            createMessage("Evolucionar 🦋 Es aceptar que tu yo del pasado era cringe", MessageCategory.GROWTH, MessageTone.HUMOROUS, "🦋"),
            
            createMessage("Tu transformación ✨ Inspira a quienes te observan en silencio", MessageCategory.GROWTH, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Eres semilla de grandeza 🌟 Tu potencial es ilimitado", MessageCategory.GROWTH, MessageTone.INSPIRATIONAL, "🌟"),
            createMessage("Tu evolución 💫 Es el regalo que le das al mundo", MessageCategory.GROWTH, MessageTone.INSPIRATIONAL, "💫"),
            
            createMessage("Growth mindset 🧠 Los errores son datos, no fracasos", MessageCategory.GROWTH, MessageTone.PRACTICAL, "🧠"),
            createMessage("Feedback loop 🔄 Acción → Resultado → Ajuste → Repetir", MessageCategory.GROWTH, MessageTone.PRACTICAL, "🔄"),
            createMessage("Aprende algo nuevo 📚 15 min diarios = expertise en un año", MessageCategory.GROWTH, MessageTone.PRACTICAL, "📚")
        ))
        
        // ============== RESILIENCE MESSAGES (30+) ==============
        messages.addAll(listOf(
            createMessage("Eres más fuerte de lo que crees 🦋 Has superado mucho", MessageCategory.RESILIENCE, MessageTone.FRIENDLY, "🦋"),
            createMessage("Las tormentas no duran para siempre 🌈 Tú sí", MessageCategory.RESILIENCE, MessageTone.FRIENDLY, "🌈"),
            createMessage("Caer es humano 💪 Levantarse es extraordinario", MessageCategory.RESILIENCE, MessageTone.FRIENDLY, "💪"),
            createMessage("Eres resiliente 🌟 Has sobrevivido al 100% de tus peores días", MessageCategory.RESILIENCE, MessageTone.FRIENDLY, "🌟"),
            createMessage("Esto también pasará 🌸 Y tú seguirás de pie", MessageCategory.RESILIENCE, MessageTone.FRIENDLY, "🌸"),
            
            createMessage("¡Levántate! 💪 Los campeones se forjan en la adversidad", MessageCategory.RESILIENCE, MessageTone.COACH, "💪"),
            createMessage("¡No te rindas! 🔥 El éxito está al otro lado del fracaso", MessageCategory.RESILIENCE, MessageTone.COACH, "🔥"),
            createMessage("¡Más fuerte! 🏆 Cada caída te prepara para una victoria mayor", MessageCategory.RESILIENCE, MessageTone.COACH, "🏆"),
            createMessage("¡Resiste! 💯 El dolor es temporal, el orgullo es permanente", MessageCategory.RESILIENCE, MessageTone.COACH, "💯"),
            
            createMessage("En la adversidad nace la fortaleza 🧘 Confía en el proceso", MessageCategory.RESILIENCE, MessageTone.WISE, "🧘"),
            createMessage("El bambú se dobla 🎋 Pero no se rompe. Sé como el bambú", MessageCategory.RESILIENCE, MessageTone.WISE, "🎋"),
            createMessage("La herida es el lugar 🌿 Por donde entra la luz", MessageCategory.RESILIENCE, MessageTone.WISE, "🌿"),
            createMessage("Kintsugi 🥣 El arte de reparar con oro. Tus cicatrices te embellecen", MessageCategory.RESILIENCE, MessageTone.WISE, "🥣"),
            
            createMessage("¡¡¡IMPARABLE!!! 💪💪💪 ¡¡¡NADA TE PUEDE DERRIBAR!!!", MessageCategory.RESILIENCE, MessageTone.ENERGETIC, "💪"),
            createMessage("¡¡¡FÉNIX!!! 🔥🔥🔥 ¡¡¡RENACE DE LAS CENIZAS!!!", MessageCategory.RESILIENCE, MessageTone.ENERGETIC, "🔥"),
            
            createMessage("Respira 🌊 Esta tormenta pasará y tú seguirás aquí", MessageCategory.RESILIENCE, MessageTone.CALM, "🌊"),
            createMessage("Calma 🍃 En el centro del huracán hay paz. Encuéntrala", MessageCategory.RESILIENCE, MessageTone.CALM, "🍃"),
            createMessage("Suavemente 🌷 Te levantas más fuerte cada vez", MessageCategory.RESILIENCE, MessageTone.CALM, "🌷"),
            
            createMessage("Resiliencia 😅 Es básicamente ser una cucaracha emocional: sobrevives a todo", MessageCategory.RESILIENCE, MessageTone.HUMOROUS, "😅"),
            createMessage("Caer 7 veces 🎯 Levantarte 8 (o 800, quién lleva la cuenta)", MessageCategory.RESILIENCE, MessageTone.HUMOROUS, "🎯"),
            
            createMessage("Eres fénix ✨ Diseñado para renacer de cualquier ceniza", MessageCategory.RESILIENCE, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Tu espíritu 🌟 Es inquebrantable, aunque a veces lo olvides", MessageCategory.RESILIENCE, MessageTone.INSPIRATIONAL, "🌟"),
            createMessage("Cada cicatriz 💫 Es evidencia de tu poder de sanación", MessageCategory.RESILIENCE, MessageTone.INSPIRATIONAL, "💫"),
            
            createMessage("Resiliencia = Perspectiva 🔄 ¿Qué aprendizaje trae esto?", MessageCategory.RESILIENCE, MessageTone.PRACTICAL, "🔄"),
            createMessage("Pequeños pasos 🦶 Cuando todo parece imposible, haz solo lo siguiente", MessageCategory.RESILIENCE, MessageTone.PRACTICAL, "🦶"),
            createMessage("Red de apoyo 🤝 Pedir ayuda es señal de fortaleza, no debilidad", MessageCategory.RESILIENCE, MessageTone.PRACTICAL, "🤝")
        ))
        
        // ============== WEEKEND MESSAGES (25+) ==============
        messages.addAll(listOf(
            createMessage("¡Feliz fin de semana! 🎊 Tiempo de recargar energías", MessageCategory.WEEKEND, MessageTone.FRIENDLY, "🎊"),
            createMessage("¡Es sábado! 🌟 Haz algo que te haga feliz hoy", MessageCategory.WEEKEND, MessageTone.FRIENDLY, "🌟"),
            createMessage("¡Domingo de relax! 🛋️ Descansa sin culpa, te lo mereces", MessageCategory.WEEKEND, MessageTone.FRIENDLY, "🛋️"),
            createMessage("Fin de semana 🎉 Tiempo para ti y los que amas", MessageCategory.WEEKEND, MessageTone.FRIENDLY, "🎉"),
            createMessage("¡Weekend mode! 🏖️ Desconéctate y recarga", MessageCategory.WEEKEND, MessageTone.FRIENDLY, "🏖️"),
            
            createMessage("Fin de semana 💪 Incluso los campeones descansan estratégicamente", MessageCategory.WEEKEND, MessageTone.COACH, "💪"),
            createMessage("Recuperación activa 🏆 Muévete, pero disfruta", MessageCategory.WEEKEND, MessageTone.COACH, "🏆"),
            
            createMessage("El descanso es sagrado 🧘 Honra tu necesidad de quietud", MessageCategory.WEEKEND, MessageTone.WISE, "🧘"),
            createMessage("Fin de semana 🌿 Tiempo para reconectarte contigo mismo", MessageCategory.WEEKEND, MessageTone.WISE, "🌿"),
            
            createMessage("¡¡¡WEEKEND!!! 🎉🎉🎉 ¡¡¡A DISFRUTAR SE HA DICHO!!!", MessageCategory.WEEKEND, MessageTone.ENERGETIC, "🎉"),
            createMessage("¡¡¡SÁBADO!!! 🌟🌟🌟 ¡¡¡VIVE AL MÁXIMO!!!", MessageCategory.WEEKEND, MessageTone.ENERGETIC, "🌟"),
            
            createMessage("Fin de semana 🌊 Fluye con la calma que mereces", MessageCategory.WEEKEND, MessageTone.CALM, "🌊"),
            createMessage("Domingo sereno 🍃 Respira la paz del descanso", MessageCategory.WEEKEND, MessageTone.CALM, "🍃"),
            
            createMessage("Fin de semana 😂 El único momento donde no hacer nada es productivo", MessageCategory.WEEKEND, MessageTone.HUMOROUS, "😂"),
            createMessage("Sábado 🛋️ El día que 'solo 5 minutos más' dura 3 horas", MessageCategory.WEEKEND, MessageTone.HUMOROUS, "🛋️"),
            createMessage("Domingo 📺 El día antes del lunes, qué trauma", MessageCategory.WEEKEND, MessageTone.HUMOROUS, "📺"),
            
            createMessage("Fin de semana mágico ✨ Llena tu alma de experiencias", MessageCategory.WEEKEND, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Estos días 🌈 Son para recordar por qué trabajas tan duro", MessageCategory.WEEKEND, MessageTone.INSPIRATIONAL, "🌈"),
            
            createMessage("Planifica algo especial 📋 Aunque sea un paseo o una película", MessageCategory.WEEKEND, MessageTone.PRACTICAL, "📋"),
            createMessage("Balance 🎯 Algo de descanso + algo de movimiento = weekend perfecto", MessageCategory.WEEKEND, MessageTone.PRACTICAL, "🎯")
        ))
        
        // ============== MONDAY MESSAGES (25+) ==============
        messages.addAll(listOf(
            createMessage("¡Feliz lunes! 💼 Nueva semana, nuevas oportunidades", MessageCategory.MONDAY, MessageTone.FRIENDLY, "💼"),
            createMessage("¡Arriba! 🌅 El lunes es el día con más potencial de la semana", MessageCategory.MONDAY, MessageTone.FRIENDLY, "🌅"),
            createMessage("Lunes = Reset 🔄 Borrón y cuenta nueva", MessageCategory.MONDAY, MessageTone.FRIENDLY, "🔄"),
            createMessage("¡Nueva semana! 🚀 Estás listo para conquistarla", MessageCategory.MONDAY, MessageTone.FRIENDLY, "🚀"),
            createMessage("Lunes bendito 🌟 7 días llenos de posibilidades te esperan", MessageCategory.MONDAY, MessageTone.FRIENDLY, "🌟"),
            
            createMessage("¡LUNES! 💪 Los ganadores aman los lunes. ¿Y tú?", MessageCategory.MONDAY, MessageTone.COACH, "💪"),
            createMessage("¡Nueva semana! 🔥 52 oportunidades al año de empezar fuerte", MessageCategory.MONDAY, MessageTone.COACH, "🔥"),
            createMessage("¡A trabajar! 🏆 El éxito no descansa, tú tampoco", MessageCategory.MONDAY, MessageTone.COACH, "🏆"),
            createMessage("¡Lunes! 💯 Mientras otros se quejan, tú conquistas", MessageCategory.MONDAY, MessageTone.COACH, "💯"),
            
            createMessage("El lunes es un regalo 🧘 Un nuevo comienzo cada semana", MessageCategory.MONDAY, MessageTone.WISE, "🧘"),
            createMessage("Nuevo ciclo 🌿 La naturaleza renueva, tú también puedes", MessageCategory.MONDAY, MessageTone.WISE, "🌿"),
            
            createMessage("¡¡¡LUNES ÉPICO!!! 💪💪💪 ¡¡¡A ROMPERLA ESTA SEMANA!!!", MessageCategory.MONDAY, MessageTone.ENERGETIC, "💪"),
            createMessage("¡¡¡NUEVA SEMANA!!! 🔥🔥🔥 ¡¡¡MODO BESTIA ACTIVADO!!!", MessageCategory.MONDAY, MessageTone.ENERGETIC, "🔥"),
            
            createMessage("Lunes tranquilo 🌊 Empieza suave para terminar fuerte", MessageCategory.MONDAY, MessageTone.CALM, "🌊"),
            createMessage("Nueva semana 🍃 Fluye con calma hacia tus objetivos", MessageCategory.MONDAY, MessageTone.CALM, "🍃"),
            
            createMessage("Lunes 😅 El día que todos fingen sorprenderse de que llegó", MessageCategory.MONDAY, MessageTone.HUMOROUS, "😅"),
            createMessage("¡Feliz lunes! ☕ OK, feliz es mucho, pero aquí estamos", MessageCategory.MONDAY, MessageTone.HUMOROUS, "☕"),
            createMessage("Lunes 😴 El día que inventó el café y la fuerza de voluntad", MessageCategory.MONDAY, MessageTone.HUMOROUS, "😴"),
            
            createMessage("Lunes mágico ✨ Esta semana será extraordinaria", MessageCategory.MONDAY, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Nueva semana 🌈 El universo te da otra oportunidad de brillar", MessageCategory.MONDAY, MessageTone.INSPIRATIONAL, "🌈"),
            
            createMessage("Planifica tu semana 📋 30 min de planificación = semana 10x más productiva", MessageCategory.MONDAY, MessageTone.PRACTICAL, "📋"),
            createMessage("Prioridades 🎯 Define tus 3 must-do de la semana", MessageCategory.MONDAY, MessageTone.PRACTICAL, "🎯")
        ))
        
        // ============== MILESTONE MESSAGES (25+) ==============
        messages.addAll(listOf(
            createMessage("¡Hito alcanzado! 🎯 Tu constancia está dando frutos", MessageCategory.MILESTONE, MessageTone.FRIENDLY, "🎯"),
            createMessage("¡Gran logro! 🏅 Este es un momento para recordar", MessageCategory.MILESTONE, MessageTone.FRIENDLY, "🏅"),
            createMessage("¡Milestone desbloqueado! 🔓 Sigue subiendo de nivel", MessageCategory.MILESTONE, MessageTone.FRIENDLY, "🔓"),
            createMessage("¡Punto de control! 📍 Mira lo lejos que has llegado", MessageCategory.MILESTONE, MessageTone.FRIENDLY, "📍"),
            createMessage("¡Nuevo nivel! 🎮 Tu progreso es impresionante", MessageCategory.MILESTONE, MessageTone.FRIENDLY, "🎮"),
            
            createMessage("¡HITO! 🏆 Esto es lo que pasa cuando no te rindes", MessageCategory.MILESTONE, MessageTone.COACH, "🏆"),
            createMessage("¡LOGRO ÉPICO! 💪 Marca este momento, es histórico", MessageCategory.MILESTONE, MessageTone.COACH, "💪"),
            createMessage("¡NIVEL UP! 🔥 Ahora ve por el siguiente desafío", MessageCategory.MILESTONE, MessageTone.COACH, "🔥"),
            
            createMessage("Cada hito es un escalón 🧘 Hacia tu mejor versión", MessageCategory.MILESTONE, MessageTone.WISE, "🧘"),
            createMessage("Este logro 🌿 Es evidencia de tu crecimiento interior", MessageCategory.MILESTONE, MessageTone.WISE, "🌿"),
            
            createMessage("¡¡¡MILESTONE!!! 🎯🎯🎯 ¡¡¡LO LOGRASTE!!!", MessageCategory.MILESTONE, MessageTone.ENERGETIC, "🎯"),
            createMessage("¡¡¡LEVEL UP!!! 🔥🔥🔥 ¡¡¡ERES IMPARABLE!!!", MessageCategory.MILESTONE, MessageTone.ENERGETIC, "🔥"),
            
            createMessage("Hito alcanzado 🌸 Celebra con gratitud y sigue adelante", MessageCategory.MILESTONE, MessageTone.CALM, "🌸"),
            createMessage("Logro sereno 🌊 En la calma, aprecias más tus victorias", MessageCategory.MILESTONE, MessageTone.CALM, "🌊"),
            
            createMessage("Milestone 😎 Básicamente eres el main character ahora", MessageCategory.MILESTONE, MessageTone.HUMOROUS, "😎"),
            createMessage("Nivel desbloqueado 🎮 ¿Achievement unlocked? More like Legend unlocked", MessageCategory.MILESTONE, MessageTone.HUMOROUS, "🎮"),
            
            createMessage("Este hito ✨ Es el preludio de tu grandeza total", MessageCategory.MILESTONE, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Milestone 🌟 Marcas el camino para quienes vienen detrás", MessageCategory.MILESTONE, MessageTone.INSPIRATIONAL, "🌟"),
            
            createMessage("Documenta este logro 📸 Las fotos de progreso motivan en días difíciles", MessageCategory.MILESTONE, MessageTone.PRACTICAL, "📸"),
            createMessage("Analiza 📊 ¿Qué estrategias te llevaron a este hito?", MessageCategory.MILESTONE, MessageTone.PRACTICAL, "📊")
        ))
        
        // ============== COMEBACK MESSAGES (25+) ==============
        messages.addAll(listOf(
            createMessage("¡Bienvenido de vuelta! 🔄 Nunca es tarde para retomar", MessageCategory.COMEBACK, MessageTone.FRIENDLY, "🔄"),
            createMessage("¡Regresaste! 🌟 Eso ya es una victoria", MessageCategory.COMEBACK, MessageTone.FRIENDLY, "🌟"),
            createMessage("El comeback más épico 💪 Empieza ahora mismo", MessageCategory.COMEBACK, MessageTone.FRIENDLY, "💪"),
            createMessage("¡De vuelta al ruedo! 🏃 Tu pausa no define tu potencial", MessageCategory.COMEBACK, MessageTone.FRIENDLY, "🏃"),
            createMessage("Nuevo comienzo 🌅 Cada día es una oportunidad de volver a empezar", MessageCategory.COMEBACK, MessageTone.FRIENDLY, "🌅"),
            
            createMessage("¡REGRESASTE! 💪 Los verdaderos campeones siempre vuelven", MessageCategory.COMEBACK, MessageTone.COACH, "💪"),
            createMessage("¡COMEBACK! 🔥 Este es tu momento de demostrar de qué estás hecho", MessageCategory.COMEBACK, MessageTone.COACH, "🔥"),
            createMessage("¡DE VUELTA! 🏆 Los ganadores no se rinden, solo toman pausas estratégicas", MessageCategory.COMEBACK, MessageTone.COACH, "🏆"),
            
            createMessage("Volver a empezar 🧘 Es un acto de valentía y sabiduría", MessageCategory.COMEBACK, MessageTone.WISE, "🧘"),
            createMessage("El regreso 🌿 Es más poderoso que el inicio", MessageCategory.COMEBACK, MessageTone.WISE, "🌿"),
            createMessage("Retomar el camino 🙏 Demuestra que el viaje importa más que el destino", MessageCategory.COMEBACK, MessageTone.WISE, "🙏"),
            
            createMessage("¡¡¡COMEBACK ÉPICO!!! 🔥🔥🔥 ¡¡¡EL REGRESO DEL CAMPEÓN!!!", MessageCategory.COMEBACK, MessageTone.ENERGETIC, "🔥"),
            createMessage("¡¡¡DE VUELTA!!! 💪💪💪 ¡¡¡MÁS FUERTE QUE NUNCA!!!", MessageCategory.COMEBACK, MessageTone.ENERGETIC, "💪"),
            
            createMessage("Bienvenido 🌸 Con calma, retoma tu ritmo", MessageCategory.COMEBACK, MessageTone.CALM, "🌸"),
            createMessage("Regreso sereno 🌊 No hay prisa, solo presencia", MessageCategory.COMEBACK, MessageTone.CALM, "🌊"),
            
            createMessage("¡Volviste! 😅 Tu yo del pasado te extrañaba (y juzgaba un poquito)", MessageCategory.COMEBACK, MessageTone.HUMOROUS, "😅"),
            createMessage("Comeback 💪 La versión remasterizada siempre es mejor", MessageCategory.COMEBACK, MessageTone.HUMOROUS, "💪"),
            createMessage("De vuelta 🔄 Como un ex, pero en versión productiva", MessageCategory.COMEBACK, MessageTone.HUMOROUS, "🔄"),
            
            createMessage("Tu regreso ✨ Inspira a quienes pensaron en rendirse", MessageCategory.COMEBACK, MessageTone.INSPIRATIONAL, "✨"),
            createMessage("Volver a empezar 🌈 Es el acto más valiente que existe", MessageCategory.COMEBACK, MessageTone.INSPIRATIONAL, "🌈"),
            createMessage("Tu comeback 💫 Será una historia de superación legendaria", MessageCategory.COMEBACK, MessageTone.INSPIRATIONAL, "💫"),
            
            createMessage("Retoma con calma 📋 Empieza con una meta pequeña y crece desde ahí", MessageCategory.COMEBACK, MessageTone.PRACTICAL, "📋"),
            createMessage("Nuevo comienzo 🎯 Define UNA cosa que harás hoy para avanzar", MessageCategory.COMEBACK, MessageTone.PRACTICAL, "🎯"),
            createMessage("Comeback strategy 🧠 Menor intensidad, mayor consistencia al inicio", MessageCategory.COMEBACK, MessageTone.PRACTICAL, "🧠")
        ))
        
        return messages
    }
    
    private var messageIndex = 0

    /**
     * Mensajes que usan los marcadores de personalización.
     *
     * Se mantienen aparte del resto de la semilla porque se insertan también en
     * instalaciones que ya tenían la tabla poblada: la semilla original sólo se
     * inserta cuando la tabla está vacía, así que sin esta lista los usuarios
     * existentes nunca verían un mensaje personalizado.
     *
     * Los marcadores los resuelve
     * [com.momentummm.app.notification.MessagePersonalizer]. Si el usuario no ha
     * configurado su nombre, `{nombre}` se elimina y la frase sigue leyéndose
     * bien; por eso todas están escritas para funcionar con y sin nombre.
     */
    fun getPersonalizedMessages(): List<MotivationalMessage> = listOf(
        personalized(
            "p_morning_1",
            "{saludo}, {nombre}. Hoy tus horas son tuyas: elige bien la primera.",
            MessageCategory.MORNING, MessageTone.FRIENDLY, "🌅"
        ),
        personalized(
            "p_morning_2",
            "Arriba, {nombre} 🌞 Nivel {nivel} y contando. Que hoy también cuente.",
            MessageCategory.MORNING, MessageTone.COACH, "🌞"
        ),
        personalized(
            "p_morning_3",
            "{saludo}. Ayer terminaste el día con {pantalla} de pantalla. Hoy decides tú.",
            MessageCategory.MORNING, MessageTone.WISE, "🌱"
        ),
        personalized(
            "p_evening_1",
            "Buenas noches, {nombre}. Hoy has usado el teléfono {pantalla}. Mañana, otra oportunidad.",
            MessageCategory.EVENING, MessageTone.WISE, "🌙"
        ),
        personalized(
            "p_evening_2",
            "{nombre}, cierra el día tranquilo: llevas {racha} días cuidando tu tiempo.",
            MessageCategory.EVENING, MessageTone.FRIENDLY, "✨"
        ),
        personalized(
            "p_streak_1",
            "{racha} días seguidos, {nombre}. Eso ya no es suerte, es un hábito.",
            MessageCategory.STREAK, MessageTone.COACH, "🔥"
        ),
        personalized(
            "p_focus_1",
            "{nombre}, ahora mismo: una sola cosa, sin pantalla. Empieza por ahí.",
            MessageCategory.FOCUS, MessageTone.PRACTICAL, "🎯"
        ),
        personalized(
            "p_motivation_1",
            "Vas por el nivel {nivel} y {monedas} monedas de tiempo, {nombre}. Sigue reclamando tus horas.",
            MessageCategory.MOTIVATION, MessageTone.FRIENDLY, "💪"
        ),
        personalized(
            "p_productivity_1",
            "{pantalla} de pantalla hoy, {nombre}. ¿Cuánto de eso elegiste tú?",
            MessageCategory.PRODUCTIVITY, MessageTone.WISE, "📱"
        ),
        personalized(
            "p_mindfulness_1",
            "{saludo}, {nombre}. Respira. El móvil puede esperar dos minutos.",
            MessageCategory.MINDFULNESS, MessageTone.WISE, "🧘"
        )
    )

    private fun personalized(
        id: String,
        content: String,
        category: MessageCategory,
        tone: MessageTone,
        emoji: String
    ): MotivationalMessage = MotivationalMessage(
        id = "personalized_$id",
        content = content,
        category = category,
        tone = tone,
        emoji = emoji,
        language = "es",
        createdAt = Date(),
        updatedAt = Date()
    )

    private fun createMessage(
        content: String,
        category: MessageCategory,
        tone: MessageTone,
        emoji: String? = null
    ): MotivationalMessage {
        messageIndex++
        return MotivationalMessage(
            id = "seed_${category.name.lowercase()}_${tone.name.lowercase()}_$messageIndex",
            content = content,
            category = category,
            tone = tone,
            emoji = emoji,
            isFavorite = false,
            timesShown = 0,
            lastShownAt = null,
            isCustom = false,
            isAIGenerated = false,
            loveCount = 0,
            shareCount = 0,
            language = "es",
            createdAt = Date(),
            updatedAt = Date()
        )
    }
}

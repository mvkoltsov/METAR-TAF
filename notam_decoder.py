#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
🔤 Декодер NOTAM в человеко-читаемый формат
Преобразует авиационные NOTAM в понятный текст на русском языке

Использование:
    python3 notam_decoder.py "текст NOTAM"
    python3 notam_decoder.py --file notam_data.json
    python3 notam_decoder.py --file notam_data.json --icao UAAA
"""

import re
import json
import argparse
from datetime import datetime
from typing import Dict, List, Optional

# ========================================
# СПРАВОЧНИКИ Q-КОДОВ
# ========================================

Q_CODES = {
    # Аэродромы
    'QFALC': 'аэродром закрыт',
    'QFALT': 'альтернативный аэродром доступен',
    'QFAXX': 'аэродром: прочее',
    
    # ВПП (Runway)
    'QMRLC': 'ВПП закрыта',
    'QMRLT': 'ВПП частично закрыта',
    'QMRXX': 'ВПП: прочее',
    'QMRAS': 'ВПП: длина сокращена',
    'QMRCC': 'ВПП: состояние покрытия изменено',
    
    # Рулёжные дорожки (Taxiway)
    'QMXLC': 'рулёжная дорожка закрыта',
    'QMXLT': 'рулёжная дорожка частично закрыта',
    'QMXXX': 'рулёжная дорожка: прочее',
    
    # Перроны и стоянки (Apron/Parking)
    'QMALC': 'перрон закрыт',
    'QMALT': 'перрон частично закрыт',
    'QMAXX': 'перрон: прочее',
    
    # Огни аэродрома (Lights)
    'QMGXX': 'огни аэродрома: прочее',
    'QMGLU': 'огни ВПП не работают',
    'QMGLT': 'огни рулёжных дорожек не работают',
    'QMGLA': 'огни перрона не работают',
    
    # Навигационные средства (Navaids)
    'QNIAS': 'ILS не работает',
    'QNIAT': 'ILS ограничено работает',
    'QNIXX': 'ILS: прочее',
    'QNVXX': 'VOR: прочее',
    'QNVAU': 'VOR не работает',
    'QNDXX': 'DME: прочее',
    'QNDAU': 'DME не работает',
    'QNNXX': 'NDB: прочее',
    'QNNAU': 'NDB не работает',
    
    # Воздушное пространство (Airspace)
    'QRRCA': 'ограничение воздушного пространства активно',
    'QRRCT': 'временное ограничение воздушного пространства',
    'QRPCA': 'запретная зона активна',
    'QRDCA': 'опасная зона активна',
    'QRTCA': 'временная зона активна',
    'QRAXX': 'воздушное пространство: прочее',
    
    # Препятствия (Obstacles)
    'QOBXX': 'препятствие: прочее',
    'QOBCE': 'препятствие установлено',
    'QOBCL': 'препятствие освещено',
    
    # Связь (Communications)
    'QFAXX': 'средства связи: прочее',
    'QFPXX': 'процедуры полётов: прочее',
    
    # Метеорология
    'QWFXX': 'прогноз погоды: прочее',
    'QWEAU': 'метеостанция не работает',
    
    # Услуги (Services)
    'QSAXX': 'аэронавигационное обслуживание: прочее',
    'QSXXX': 'службы: прочее',
    'QSFAU': 'топливо недоступно',
    'QSUAS': 'поисково-спасательная служба: прочее',
    'QSGAS': 'обслуживание наземной техникой ограничено',
    
    # Прочее
    'QXXXX': 'прочее',
}

# ========================================
# СПРАВОЧНИК АВИАЦИОННЫХ АББРЕВИАТУР
# ========================================

ABBREVIATIONS = {
    # Аэродромные объекты
    'RWY': 'ВПП',
    'TWY': 'рулёжная дорожка',
    'APRON': 'перрон',
    'TERMINAL': 'терминал',
    'PARKING': 'стоянка',
    
    # Состояния
    'CLSD': 'закрыт',
    'CLOSED': 'закрыт',
    'OPEN': 'открыт',
    'AVBL': 'доступен',
    'AVAILABLE': 'доступен',
    'U/S': 'не работает',
    'UNSERVICEABLE': 'не работает',
    'OPS': 'эксплуатация',
    'OPR': 'работает',
    'OPERATIONAL': 'в работе',
    
    # Причины
    'MAINT': 'техобслуживание',
    'MAINTENANCE': 'техобслуживание',
    'WIP': 'строительные работы',
    'WORK IN PROGRESS': 'строительные работы',
    'CONST': 'строительство',
    'CONSTRUCTION': 'строительство',
    'REPAIR': 'ремонт',
    'INSP': 'инспекция',
    'INSPECTION': 'инспекция',
    
    # Навигация
    'ILS': 'система инструментальной посадки',
    'VOR': 'всенаправленный радиомаяк',
    'DME': 'дальномерное оборудование',
    'NDB': 'ненаправленный радиомаяк',
    'PAPI': 'индикатор глиссады',
    'VASIS': 'визуальная система захода на посадку',
    
    # Огни
    'LGT': 'огни',
    'LIGHTS': 'огни',
    'ALS': 'огни захода на посадку',
    'EDGE': 'кромочные огни',
    'CL': 'осевые огни',
    'CENTERLINE': 'осевые огни',
    
    # Высоты
    'SFC': 'поверхность',
    'GND': 'земля',
    'AGL': 'над уровнем земли',
    'AMSL': 'над уровнем моря',
    'FT': 'футов',
    'FL': 'эшелон',
    
    # Время
    'DAILY': 'ежедневно',
    'MON': 'понедельник',
    'TUE': 'вторник',
    'WED': 'среда',
    'THU': 'четверг',
    'FRI': 'пятница',
    'SAT': 'суббота',
    'SUN': 'воскресенье',
    'UTC': 'всемирное время',
    'PERM': 'постоянно',
    'TEMPO': 'временно',
    
    # Прочее
    'INFO': 'информация',
    'ADZ': 'зона аэродрома',
    'CTR': 'диспетчерская зона',
    'FIR': 'район полётной информации',
    'TMA': 'диспетчерский район',
    'FREQ': 'частота',
    'ATIS': 'автоматическая информация',
}

# ========================================
# ДЕКОДЕР NOTAM
# ========================================

class NOTAMDecoder:
    """Декодер NOTAM в человеко-читаемый формат"""
    
    def __init__(self):
        pass
    
    def decode(self, notam_data: Dict) -> Dict:
        """
        Декодирование NOTAM
        
        Args:
            notam_data: Словарь с данными NOTAM
            
        Returns:
            Словарь с декодированными данными
        """
        decoded = {
            'id': notam_data.get('id'),
            'type': notam_data.get('type'),
            'type_text': self._decode_type(notam_data.get('type')),
            'severity': None,
            'severity_emoji': None,
            'q_code': notam_data.get('q_code'),
            'q_decoded': None,
            'location': notam_data.get('location'),
            'location_name': None,
            'valid_from': notam_data.get('valid_from'),
            'valid_to': notam_data.get('valid_to'),
            'is_permanent': notam_data.get('is_permanent', False),
            'schedule': notam_data.get('schedule'),
            'description_raw': notam_data.get('description_raw'),
            'description_decoded': None,
            'lower_limit': notam_data.get('lower_limit'),
            'upper_limit': notam_data.get('upper_limit'),
            'human_readable': '',
            'raw': notam_data.get('raw', '')
        }
        
        # Декодируем Q-код
        if decoded['q_code']:
            decoded['q_decoded'] = Q_CODES.get(decoded['q_code'], decoded['q_code'])
            decoded['severity'] = self._classify_severity(decoded['q_code'])
            decoded['severity_emoji'] = self._get_severity_emoji(decoded['severity'])
        
        # Декодируем описание
        if decoded['description_raw']:
            decoded['description_decoded'] = self._decode_description(decoded['description_raw'])
        
        # Получаем название локации
        decoded['location_name'] = self._get_location_name(decoded['location'])
        
        # Генерируем человеко-читаемый текст
        decoded['human_readable'] = self._generate_human_text(decoded)
        
        return decoded
    
    def _decode_type(self, notam_type: str) -> str:
        """Декодирование типа NOTAM"""
        types = {
            'N': 'новый',
            'R': 'замена',
            'C': 'отмена',
        }
        return types.get(notam_type, notam_type)
    
    def _classify_severity(self, q_code: str) -> str:
        """
        Классификация критичности NOTAM
        
        Returns:
            'critical', 'warning', или 'info'
        """
        # Критичные коды (закрытие аэродрома, ВПП, важных систем)
        critical_patterns = [
            'QFALC',  # аэродром закрыт
            'QMRLC',  # ВПП закрыта
            'QMXLC',  # РД закрыта
            'QNIAS',  # ILS не работает
        ]
        
        # Предупреждающие коды (частичные закрытия, ограничения)
        warning_patterns = [
            'QMRLT',  # ВПП частично закрыта
            'QMALC',  # перрон закрыт
            'QMGLU',  # огни ВПП
            'QRRCA',  # ограничение воздушного пространства
            'QOBXX',  # препятствия
        ]
        
        if q_code in critical_patterns:
            return 'critical'
        
        for pattern in warning_patterns:
            if q_code.startswith(pattern[:4]):
                return 'warning'
        
        return 'info'
    
    def _get_severity_emoji(self, severity: str) -> str:
        """Получение emoji для критичности"""
        emojis = {
            'critical': '🔴',
            'warning': '🟡',
            'info': '🔵',
        }
        return emojis.get(severity, '⚪')
    
    def _decode_description(self, description: str) -> str:
        """
        Декодирование описания NOTAM с заменой аббревиатур
        
        Args:
            description: Исходное описание
            
        Returns:
            Декодированное описание
        """
        decoded = description
        
        # Заменяем аббревиатуры
        for abbr, translation in ABBREVIATIONS.items():
            # Используем границы слов для точной замены
            pattern = r'\b' + re.escape(abbr) + r'\b'
            decoded = re.sub(pattern, translation, decoded, flags=re.IGNORECASE)
        
        return decoded
    
    def _get_location_name(self, location: str) -> Optional[str]:
        """Получение названия аэродрома"""
        from collect_notam import AIRPORTS
        
        airport_info = AIRPORTS.get(location)
        if airport_info:
            return airport_info['name']
        
        return location
    
    def _generate_human_text(self, decoded: Dict) -> str:
        """
        Генерация человеко-читаемого текста
        
        Args:
            decoded: Декодированные данные
            
        Returns:
            Форматированный текст
        """
        lines = []
        
        # Заголовок с emoji критичности
        severity_emoji = decoded['severity_emoji'] or '📋'
        lines.append(f"{severity_emoji} NOTAM {decoded['id']} ({decoded['type_text']})")
        lines.append("")
        
        # Аэродром
        if decoded['location_name']:
            lines.append(f"📍 Аэродром: {decoded['location']} - {decoded['location_name']}")
        else:
            lines.append(f"📍 Локация: {decoded['location']}")
        
        # Тип ограничения
        if decoded['q_decoded']:
            lines.append(f"⚠️  Тип: {decoded['q_decoded']}")
        
        # Период действия
        if decoded['is_permanent']:
            lines.append(f"⏰ Действует: ПОСТОЯННО")
        else:
            valid_from = self._format_datetime(decoded['valid_from'])
            valid_to = self._format_datetime(decoded['valid_to'])
            lines.append(f"⏰ Действует: с {valid_from} до {valid_to}")
        
        # Расписание
        if decoded['schedule']:
            lines.append(f"📅 Расписание: {decoded['schedule']}")
        
        # Высоты
        if decoded['lower_limit'] or decoded['upper_limit']:
            lower = decoded['lower_limit'] or 'N/A'
            upper = decoded['upper_limit'] or 'N/A'
            lines.append(f"📏 Высоты: {lower} - {upper}")
        
        lines.append("")
        lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        lines.append("📝 ОПИСАНИЕ:")
        lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        lines.append("")
        
        # Описание
        if decoded['description_decoded']:
            lines.append(decoded['description_decoded'])
        elif decoded['description_raw']:
            lines.append(decoded['description_raw'])
        
        # Классификация критичности
        lines.append("")
        lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        severity_text = {
            'critical': '🔴 КРИТИЧНО - требует немедленного внимания',
            'warning': '🟡 ПРЕДУПРЕЖДЕНИЕ - требует внимания',
            'info': '🔵 ИНФОРМАЦИЯ',
        }
        lines.append(severity_text.get(decoded['severity'], ''))
        
        return '\n'.join(lines)
    
    def _format_datetime(self, dt_str: Optional[str]) -> str:
        """
        Форматирование даты/времени для отображения
        
        Args:
            dt_str: ISO формат или строка
            
        Returns:
            Форматированная строка
        """
        if not dt_str:
            return 'N/A'
        
        if dt_str == 'EST':
            return 'уточняется'
        
        try:
            # Парсим ISO формат
            if 'T' in dt_str:
                dt = datetime.fromisoformat(dt_str.replace('Z', '+00:00'))
                return dt.strftime('%d.%m.%Y %H:%M UTC')
        except Exception:
            pass
        
        return dt_str


# ========================================
# ФУНКЦИИ ДЛЯ РАБОТЫ С ФАЙЛАМИ
# ========================================

def decode_notam_file(json_file: str, icao: Optional[str] = None) -> List[Dict]:
    """
    Декодирование NOTAM из JSON файла
    
    Args:
        json_file: Путь к JSON файлу
        icao: Фильтр по ICAO (опционально)
        
    Returns:
        Список декодированных NOTAM
    """
    with open(json_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    decoder = NOTAMDecoder()
    decoded_list = []
    
    notams = data.get('notams', [])
    
    # Фильтрация по ICAO
    if icao:
        notams = [n for n in notams if n.get('location') == icao.upper()]
    
    for notam in notams:
        decoded = decoder.decode(notam)
        decoded_list.append(decoded)
    
    return decoded_list


# ========================================
# CLI
# ========================================

def main():
    parser = argparse.ArgumentParser(
        description='Декодер NOTAM в человеко-читаемый формат',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Примеры:

  # Декодирование из JSON файла
  python3 notam_decoder.py --file notam_data.json
  
  # Декодирование для конкретного аэродрома
  python3 notam_decoder.py --file notam_data.json --icao UAAA
  
  # Вывод в JSON формате
  python3 notam_decoder.py --file notam_data.json --json
  
  # Сохранение в файл
  python3 notam_decoder.py --file notam_data.json --output decoded.txt
        """
    )
    
    parser.add_argument(
        '--file', '-f',
        type=str,
        required=True,
        help='JSON файл с данными NOTAM'
    )
    
    parser.add_argument(
        '--icao',
        type=str,
        help='Фильтр по ICAO коду'
    )
    
    parser.add_argument(
        '--json',
        action='store_true',
        help='Вывод в JSON формате'
    )
    
    parser.add_argument(
        '--output', '-o',
        type=str,
        help='Сохранить результат в файл'
    )
    
    args = parser.parse_args()
    
    # Декодирование
    try:
        decoded_list = decode_notam_file(args.file, args.icao)
        
        if not decoded_list:
            print("❌ NOTAM не найдены")
            return
        
        if args.json:
            # JSON вывод
            output = json.dumps(decoded_list, ensure_ascii=False, indent=2)
            
            if args.output:
                with open(args.output, 'w', encoding='utf-8') as f:
                    f.write(output)
                print(f"✅ Результат сохранён в: {args.output}")
            else:
                print(output)
        else:
            # Текстовый вывод
            print("\n╔════════════════════════════════════════════════════════════╗")
            print("║  📋 ДЕКОДИРОВАННЫЕ NOTAM                                   ║")
            print("╚════════════════════════════════════════════════════════════╝")
            print()
            
            for i, decoded in enumerate(decoded_list, 1):
                print(f"\n{'═' * 60}")
                print(f"NOTAM {i} из {len(decoded_list)}")
                print('═' * 60)
                print()
                print(decoded['human_readable'])
                print()
            
            # Сохранение в файл
            if args.output:
                with open(args.output, 'w', encoding='utf-8') as f:
                    for i, decoded in enumerate(decoded_list, 1):
                        f.write(f"\n{'═' * 60}\n")
                        f.write(f"NOTAM {i} из {len(decoded_list)}\n")
                        f.write('═' * 60 + '\n\n')
                        f.write(decoded['human_readable'] + '\n\n')
                
                print(f"✅ Результат сохранён в: {args.output}")
    
    except FileNotFoundError:
        print(f"❌ Файл не найден: {args.file}")
    except Exception as e:
        print(f"❌ Ошибка: {e}")
        import traceback
        traceback.print_exc()


if __name__ == '__main__':
    main()

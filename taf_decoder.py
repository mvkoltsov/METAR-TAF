#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
🔤 Декодер TAF в человеко-читаемый формат
Преобразует авиационные прогнозы TAF в понятный текст

Использование:
    python3 taf_decoder.py "TAF UAAA 101100Z ..."
    python3 taf_decoder.py --file taf_data.json
"""

import re
import json
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Tuple
import argparse


# ========================================
# СПРАВОЧНИКИ
# ========================================

# Облачность
CLOUD_COVER = {
    'SKC': 'ясно',
    'CLR': 'ясно',
    'NSC': 'нет значимой облачности',
    'NCD': 'нет облаков (автоматическая станция)',
    'FEW': 'малооблачно (1-2 окты)',
    'SCT': 'рассеянная облачность (3-4 окты)',
    'BKN': 'значительная облачность (5-7 окт)',
    'OVC': 'сплошная облачность (8 окт)',
    'VV': 'вертикальная видимость',
}

# Типы облаков
CLOUD_TYPES = {
    'CB': 'кучево-дождевые (грозовые)',
    'TCU': 'мощно-кучевые',
    'CI': 'перистые',
    'CC': 'перисто-кучевые',
    'CS': 'перисто-слоистые',
    'AC': 'высоко-кучевые',
    'AS': 'высоко-слоистые',
    'NS': 'слоисто-дождевые',
    'SC': 'слоисто-кучевые',
    'ST': 'слоистые',
    'CU': 'кучевые',
}

# Погодные явления
WEATHER_PHENOMENA = {
    # Интенсивность
    '-': 'слабая',
    '+': 'сильная',
    'VC': 'в окрестности',
    
    # Дескрипторы
    'MI': 'низкая',
    'BC': 'клочковатая',
    'PR': 'частичная',
    'DR': 'низовая',
    'BL': 'поземок',
    'SH': 'ливневая',
    'TS': 'гроза',
    'FZ': 'переохлажденная',
    
    # Осадки
    'DZ': 'морось',
    'RA': 'дождь',
    'SN': 'снег',
    'SG': 'снежные зёрна',
    'IC': 'ледяные кристаллы',
    'PL': 'ледяная крупа',
    'GR': 'град',
    'GS': 'мелкий град',
    'UP': 'неопределённые осадки',
    
    # Туман и дымка
    'BR': 'дымка',
    'FG': 'туман',
    'FU': 'дым',
    'VA': 'вулканический пепел',
    'DU': 'пыль',
    'SA': 'песок',
    'HZ': 'мгла',
    'PY': 'водяные брызги',
    
    # Прочее
    'PO': 'пыльные вихри',
    'SQ': 'шквал',
    'FC': 'воронкообразное облако/торнадо',
    'SS': 'песчаная буря',
    'DS': 'пыльная буря',
}

# Изменения прогноза
CHANGE_INDICATORS = {
    'FM': 'с момента',
    'TEMPO': 'временами',
    'PROB30': 'вероятность 30%',
    'PROB40': 'вероятность 40%',
    'BECMG': 'постепенное изменение',
    'NOSIG': 'без значительных изменений',
}

# Единицы измерения
UNITS = {
    'visibility_m': 'м',
    'visibility_km': 'км',
    'wind_speed_kt': 'узлов',
    'wind_speed_mps': 'м/с',
    'wind_speed_kmh': 'км/ч',
    'temperature': '°C',
    'pressure': 'гПа',
    'altitude_ft': 'футов',
    'altitude_m': 'метров',
}


# ========================================
# ДЕКОДЕР TAF
# ========================================

class TAFDecoder:
    """Декодер TAF в человеко-читаемый формат"""
    
    def __init__(self):
        self.raw_taf = ""
        self.decoded = {}
    
    def decode(self, taf_text: str) -> Dict:
        """
        Основной метод декодирования
        
        Args:
            taf_text: Сырой текст TAF
            
        Returns:
            Словарь с декодированными данными
        """
        self.raw_taf = taf_text.strip()
        self.decoded = {
            'raw': self.raw_taf,
            'human_readable': '',
            'station': None,
            'issue_time': None,
            'valid_period': None,
            'wind': None,
            'visibility': None,
            'weather': [],
            'clouds': [],
            'temperature': None,
            'changes': [],
            'remarks': None,
        }
        
        try:
            # Удаляем лишние пробелы
            taf = ' '.join(self.raw_taf.split())
            
            # Парсим основные элементы
            self._parse_header(taf)
            self._parse_main_forecast(taf)
            self._parse_changes(taf)
            
            # Генерируем человеко-читаемый текст
            self._generate_human_text()
            
        except Exception as e:
            self.decoded['error'] = f"Ошибка декодирования: {e}"
        
        return self.decoded
    
    def _parse_header(self, taf: str):
        """Парсинг заголовка TAF"""
        
        # Аэродром (ICAO код)
        station_match = re.search(r'TAF\s+(?:AMD\s+|COR\s+)?([A-Z]{4})', taf)
        if station_match:
            self.decoded['station'] = station_match.group(1)
        
        # Время выпуска (DDHHmmZ)
        issue_match = re.search(r'([0-3]\d)([0-2]\d)([0-5]\d)Z', taf)
        if issue_match:
            day = int(issue_match.group(1))
            hour = int(issue_match.group(2))
            minute = int(issue_match.group(3))
            self.decoded['issue_time'] = {
                'day': day,
                'hour': hour,
                'minute': minute,
                'text': f'{day:02d} число, {hour:02d}:{minute:02d} UTC'
            }
        
        # Период действия (DDHH/DDHH)
        valid_match = re.search(r'([0-3]\d)([0-2]\d)/([0-3]\d)([0-2]\d)', taf)
        if valid_match:
            from_day = int(valid_match.group(1))
            from_hour = int(valid_match.group(2))
            to_day = int(valid_match.group(3))
            to_hour = int(valid_match.group(4))
            
            self.decoded['valid_period'] = {
                'from_day': from_day,
                'from_hour': from_hour,
                'to_day': to_day,
                'to_hour': to_hour,
                'text': f'с {from_day:02d} числа {from_hour:02d}:00 до {to_day:02d} числа {to_hour:02d}:00 UTC'
            }
    
    def _parse_main_forecast(self, taf: str):
        """Парсинг основного прогноза"""
        
        # Находим основную часть (до первого FM/TEMPO/BECMG)
        main_part = re.split(r'\s+(FM|TEMPO|BECMG|PROB)', taf)[0]
        
        # Ветер
        self.decoded['wind'] = self._parse_wind(main_part)
        
        # Видимость
        self.decoded['visibility'] = self._parse_visibility(main_part)
        
        # Погодные явления
        self.decoded['weather'] = self._parse_weather(main_part)
        
        # Облачность
        self.decoded['clouds'] = self._parse_clouds(main_part)
        
        # Температура (опционально в TAF, но может быть)
        self.decoded['temperature'] = self._parse_temperature(main_part)
    
    def _parse_wind(self, text: str) -> Optional[Dict]:
        """Парсинг ветра"""
        
        # Штиль
        if '00000KT' in text or '00000MPS' in text:
            return {
                'speed': 0,
                'direction': None,
                'gusts': None,
                'text': '🌬️ штиль'
            }
        
        # Переменный ветер
        vrb_match = re.search(r'VRB(\d{2,3})(G(\d{2,3}))?(KT|MPS)', text)
        if vrb_match:
            speed = int(vrb_match.group(1))
            gusts = int(vrb_match.group(3)) if vrb_match.group(3) else None
            unit = vrb_match.group(4)
            
            # Конвертация в м/с
            if unit == 'KT':
                speed_mps = int(speed * 0.514)
                gusts_mps = int(gusts * 0.514) if gusts else None
            else:
                speed_mps = speed
                gusts_mps = gusts
            
            gust_text = f', порывы до {gusts_mps} м/с' if gusts else ''
            
            return {
                'speed': speed_mps,
                'direction': 'переменное',
                'gusts': gusts_mps,
                'text': f'🌬️ ветер переменного направления {speed_mps} м/с{gust_text}'
            }
        
        # Обычный ветер
        wind_match = re.search(r'(\d{3})(\d{2,3})(G(\d{2,3}))?(KT|MPS)', text)
        if wind_match:
            direction = int(wind_match.group(1))
            speed = int(wind_match.group(2))
            gusts = int(wind_match.group(4)) if wind_match.group(4) else None
            unit = wind_match.group(5)
            
            # Конвертация
            if unit == 'KT':
                speed_mps = int(speed * 0.514)
                gusts_mps = int(gusts * 0.514) if gusts else None
            else:
                speed_mps = speed
                gusts_mps = gusts
            
            # Направление словами
            directions = ['С', 'СВ', 'В', 'ЮВ', 'Ю', 'ЮЗ', 'З', 'СЗ']
            dir_idx = int((direction + 22.5) / 45) % 8
            dir_text = directions[dir_idx]
            
            gust_text = f', порывы до {gusts_mps} м/с' if gusts else ''
            
            return {
                'speed': speed_mps,
                'direction': direction,
                'direction_text': dir_text,
                'gusts': gusts_mps,
                'text': f'🌬️ ветер {dir_text} ({direction}°) {speed_mps} м/с{gust_text}'
            }
        
        return None
    
    def _parse_visibility(self, text: str) -> Optional[Dict]:
        """Парсинг видимости"""
        
        # CAVOK - отличная видимость
        if 'CAVOK' in text:
            return {
                'meters': 10000,
                'text': '👁️ видимость более 10 км, без облачности ниже 1500м, без грозовых явлений (CAVOK)'
            }
        
        # 9999 - 10 км и более
        if '9999' in text:
            return {
                'meters': 10000,
                'text': '👁️ видимость 10 км и более'
            }
        
        # 4-значная видимость в метрах
        vis_match = re.search(r'\s(\d{4})\s', text)
        if vis_match:
            meters = int(vis_match.group(1))
            
            if meters >= 5000:
                quality = 'хорошая'
            elif meters >= 3000:
                quality = 'средняя'
            elif meters >= 1000:
                quality = 'ограниченная'
            else:
                quality = 'плохая'
            
            if meters >= 1000:
                km = meters / 1000
                text = f'👁️ видимость {km:.1f} км ({quality})'
            else:
                text = f'👁️ видимость {meters} м ({quality})'
            
            return {
                'meters': meters,
                'quality': quality,
                'text': text
            }
        
        return None
    
    def _parse_weather(self, text: str) -> List[Dict]:
        """Парсинг погодных явлений"""
        weather = []
        
        # Паттерн для погодных явлений
        # Например: -RA, +TSRA, VCSH, BR
        pattern = r'(?:^|\s)([-+]|VC)?([A-Z]{2,6})(?=\s|$)'
        
        for match in re.finditer(pattern, text):
            intensity = match.group(1) or ''
            code = match.group(2)
            
            # Пропускаем CAVOK, облачность и другие не-погодные коды
            if code in ['CAVOK', 'NSC', 'SKC', 'CLR', 'NCD']:
                continue
            if code.startswith(tuple(['FEW', 'SCT', 'BKN', 'OVC', 'VV'])):
                continue
            
            # Декодируем
            description = self._decode_weather_code(intensity + code)
            if description:
                weather.append({
                    'code': intensity + code,
                    'text': description
                })
        
        return weather
    
    def _decode_weather_code(self, code: str) -> Optional[str]:
        """Декодирование кода погодного явления"""
        parts = []
        
        # Интенсивность
        if code.startswith('-'):
            parts.append('слабый')
            code = code[1:]
        elif code.startswith('+'):
            parts.append('сильный')
            code = code[1:]
        elif code.startswith('VC'):
            parts.append('в окрестности')
            code = code[2:]
        
        # Разбиваем на двухбуквенные коды
        i = 0
        while i < len(code):
            two_char = code[i:i+2]
            if two_char in WEATHER_PHENOMENA:
                parts.append(WEATHER_PHENOMENA[two_char])
                i += 2
            else:
                i += 1
        
        if parts:
            emoji = self._get_weather_emoji(code)
            return f'{emoji} {" ".join(parts)}'
        
        return None
    
    def _get_weather_emoji(self, code: str) -> str:
        """Получение emoji для погодного явления"""
        if 'TS' in code:
            return '⛈️'
        elif 'RA' in code:
            return '🌧️'
        elif 'SN' in code:
            return '❄️'
        elif 'FG' in code:
            return '🌫️'
        elif 'BR' in code:
            return '🌫️'
        elif 'SH' in code:
            return '🌦️'
        elif 'GR' in code or 'GS' in code:
            return '🌨️'
        else:
            return '☁️'
    
    def _parse_clouds(self, text: str) -> List[Dict]:
        """Парсинг облачности"""
        clouds = []
        
        # Паттерн: FEW015, SCT020CB, BKN040
        pattern = r'(FEW|SCT|BKN|OVC|VV)(\d{3})(CB|TCU)?'
        
        for match in re.finditer(pattern, text):
            cover = match.group(1)
            height_code = match.group(2)
            cloud_type = match.group(3)
            
            # Высота в футах * 100
            height_ft = int(height_code) * 100
            height_m = int(height_ft * 0.3048)
            
            cover_text = CLOUD_COVER.get(cover, cover)
            type_text = f', {CLOUD_TYPES.get(cloud_type, cloud_type)}' if cloud_type else ''
            
            clouds.append({
                'cover': cover,
                'cover_text': cover_text,
                'height_ft': height_ft,
                'height_m': height_m,
                'type': cloud_type,
                'text': f'☁️ {cover_text} на высоте {height_m}м ({height_ft}фт){type_text}'
            })
        
        return clouds
    
    def _parse_temperature(self, text: str) -> Optional[Dict]:
        """Парсинг температуры (если есть)"""
        # TX15/1012Z TN05/1103Z
        temp_match = re.search(r'T([XN])M?(\d{2})/(\d{4})Z', text)
        if temp_match:
            temp_type = 'максимальная' if temp_match.group(1) == 'X' else 'минимальная'
            temp = int(temp_match.group(2))
            if 'M' in text:  # Минус
                temp = -temp
            
            return {
                'type': temp_type,
                'value': temp,
                'text': f'🌡️ {temp_type} температура {temp:+d}°C'
            }
        
        return None
    
    def _parse_changes(self, taf: str):
        """Парсинг изменений прогноза (FM, TEMPO, BECMG)"""
        changes = []
        
        # Разбиваем на части по индикаторам изменений
        parts = re.split(r'\s+(FM\d{6}|TEMPO|BECMG|PROB\d{2}\s+TEMPO|PROB\d{2})', taf)
        
        for i in range(1, len(parts), 2):
            if i+1 < len(parts):
                indicator = parts[i].strip()
                content = parts[i+1].strip()
                
                change = {
                    'indicator': indicator,
                    'indicator_text': self._decode_change_indicator(indicator),
                    'wind': self._parse_wind(content),
                    'visibility': self._parse_visibility(content),
                    'weather': self._parse_weather(content),
                    'clouds': self._parse_clouds(content),
                }
                
                changes.append(change)
        
        self.decoded['changes'] = changes
    
    def _decode_change_indicator(self, indicator: str) -> str:
        """Декодирование индикатора изменений"""
        if indicator.startswith('FM'):
            # FM101200
            time = indicator[2:]
            day = time[:2]
            hour = time[2:4]
            minute = time[4:6]
            return f'С {day} числа {hour}:{minute} UTC'
        elif indicator.startswith('PROB'):
            prob = indicator[4:6]
            return f'Вероятность {prob}%'
        elif indicator == 'TEMPO':
            return 'Временами'
        elif indicator == 'BECMG':
            return 'Постепенное изменение'
        
        return indicator
    
    def _generate_human_text(self):
        """Генерация человеко-читаемого текста"""
        lines = []
        
        # Заголовок
        if self.decoded['station']:
            lines.append(f"📍 АЭРОДРОМ: {self.decoded['station']}")
        
        if self.decoded['issue_time']:
            lines.append(f"📅 Выпущен: {self.decoded['issue_time']['text']}")
        
        if self.decoded['valid_period']:
            lines.append(f"⏰ Действителен: {self.decoded['valid_period']['text']}")
        
        lines.append("")
        lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        lines.append("🌤️  ОСНОВНОЙ ПРОГНОЗ")
        lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        lines.append("")
        
        # Основной прогноз
        if self.decoded['wind']:
            lines.append(self.decoded['wind']['text'])
        
        if self.decoded['visibility']:
            lines.append(self.decoded['visibility']['text'])
        
        if self.decoded['weather']:
            for wx in self.decoded['weather']:
                lines.append(wx['text'])
        
        if self.decoded['clouds']:
            for cloud in self.decoded['clouds']:
                lines.append(cloud['text'])
        elif self.decoded['visibility'] and self.decoded['visibility'].get('meters', 0) >= 10000:
            lines.append("☁️ без значимой облачности")
        
        if self.decoded['temperature']:
            lines.append(self.decoded['temperature']['text'])
        
        # Изменения
        if self.decoded['changes']:
            lines.append("")
            lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            lines.append("🔄 ОЖИДАЕМЫЕ ИЗМЕНЕНИЯ")
            lines.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            
            for i, change in enumerate(self.decoded['changes'], 1):
                lines.append("")
                lines.append(f"▸ {change['indicator_text']}:")
                
                if change['wind']:
                    lines.append(f"  {change['wind']['text']}")
                if change['visibility']:
                    lines.append(f"  {change['visibility']['text']}")
                if change['weather']:
                    for wx in change['weather']:
                        lines.append(f"  {wx['text']}")
                if change['clouds']:
                    for cloud in change['clouds']:
                        lines.append(f"  {cloud['text']}")
        
        self.decoded['human_readable'] = '\n'.join(lines)


# ========================================
# ФУНКЦИИ ДЛЯ РАБОТЫ С ФАЙЛАМИ
# ========================================

def decode_taf_text(taf_text: str) -> str:
    """Декодирование одного TAF текста"""
    decoder = TAFDecoder()
    result = decoder.decode(taf_text)
    return result['human_readable']


def decode_taf_file(json_file: str) -> List[Dict]:
    """Декодирование TAF из JSON файла"""
    with open(json_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    decoder = TAFDecoder()
    decoded_list = []
    
    for item in data.get('taf_data', []):
        if item.get('taf') and item['taf'].get('raw'):
            decoded = decoder.decode(item['taf']['raw'])
            
            decoded_list.append({
                'icao': item['icao'],
                'name': item['name'],
                'city': item['city'],
                'raw': item['taf']['raw'],
                'decoded': decoded['human_readable'],
                'full_data': decoded
            })
    
    return decoded_list


# ========================================
# CLI
# ========================================

def main():
    parser = argparse.ArgumentParser(
        description='Декодер TAF в человеко-читаемый формат',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Примеры:

  # Декодирование TAF текста
  python3 taf_decoder.py "TAF UAAA 101100Z 1012/1112 32015G25KT 9999 FEW040 BKN100"
  
  # Декодирование из JSON файла
  python3 taf_decoder.py --file taf_data.json
  
  # Вывод в JSON формате
  python3 taf_decoder.py --file taf_data.json --json
  
  # Декодирование конкретного аэродрома из файла
  python3 taf_decoder.py --file taf_data.json --icao UAAA
        """
    )
    
    parser.add_argument(
        'taf_text',
        nargs='?',
        help='Текст TAF для декодирования'
    )
    
    parser.add_argument(
        '--file', '-f',
        type=str,
        help='JSON файл с данными TAF'
    )
    
    parser.add_argument(
        '--icao',
        type=str,
        help='Фильтр по ICAO коду (для --file)'
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
    
    # Декодирование из файла
    if args.file:
        decoded_list = decode_taf_file(args.file)
        
        # Фильтр по ICAO
        if args.icao:
            decoded_list = [d for d in decoded_list if d['icao'] == args.icao.upper()]
        
        if args.json:
            # JSON вывод
            output = json.dumps(decoded_list, ensure_ascii=False, indent=2)
            print(output)
        else:
            # Текстовый вывод
            for item in decoded_list:
                print("\n" + "=" * 60)
                print(f"{item['icao']} - {item['name']} ({item['city']})")
                print("=" * 60)
                print(f"\nСЫРОЙ TAF:")
                print(item['raw'])
                print(f"\nДЕКОДИРОВАННЫЙ ПРОГНОЗ:")
                print(item['decoded'])
                print()
        
        # Сохранение в файл
        if args.output:
            with open(args.output, 'w', encoding='utf-8') as f:
                if args.json:
                    json.dump(decoded_list, f, ensure_ascii=False, indent=2)
                else:
                    for item in decoded_list:
                        f.write("\n" + "=" * 60 + "\n")
                        f.write(f"{item['icao']} - {item['name']} ({item['city']})\n")
                        f.write("=" * 60 + "\n\n")
                        f.write("СЫРОЙ TAF:\n")
                        f.write(item['raw'] + "\n\n")
                        f.write("ДЕКОДИРОВАННЫЙ ПРОГНОЗ:\n")
                        f.write(item['decoded'] + "\n\n")
            
            print(f"\n✅ Результат сохранён в: {args.output}")
    
    # Декодирование текста
    elif args.taf_text:
        decoder = TAFDecoder()
        result = decoder.decode(args.taf_text)
        
        if args.json:
            print(json.dumps(result, ensure_ascii=False, indent=2))
        else:
            print("\n" + "=" * 60)
            print("ДЕКОДИРОВАННЫЙ TAF")
            print("=" * 60 + "\n")
            print(result['human_readable'])
        
        if args.output:
            with open(args.output, 'w', encoding='utf-8') as f:
                if args.json:
                    json.dump(result, f, ensure_ascii=False, indent=2)
                else:
                    f.write(result['human_readable'])
            print(f"\n✅ Результат сохранён в: {args.output}")
    
    else:
        parser.print_help()


if __name__ == '__main__':
    main()

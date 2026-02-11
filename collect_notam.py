#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
🛫 Сбор NOTAM (Notice to Air Missions) для аэродромов Казахстана

Использование:
    python3 collect_notam.py
    python3 collect_notam.py --icao UAAA
    python3 collect_notam.py --output notam_data.json
"""

import requests
import json
import argparse
import re
import time
from datetime import datetime
from typing import List, Dict, Optional
from bs4 import BeautifulSoup

# ========================================
# БАЗА ДАННЫХ АЭРОДРОМОВ КАЗАХСТАНА
# ========================================

AIRPORTS = {
    "UAAA": {"name": "Алматы (Almaty)", "city": "Алматы"},
    "UAAT": {"name": "Талдыкорган (Taldykorgan)", "city": "Талдыкорган"},
    "UACC": {"name": "Астана (Nursultan Nazarbayev)", "city": "Астана"},
    "UATT": {"name": "Актобе (Aktobe)", "city": "Актобе"},
    "UATG": {"name": "Атырау (Atyrau)", "city": "Атырау"},
    "UARR": {"name": "Уральск (Oral/Uralsk)", "city": "Уральск"},
    "UAKK": {"name": "Караганда (Sary-Arka)", "city": "Караганда"},
    "UAAH": {"name": "Балхаш (Balkhash)", "city": "Балхаш"},
    "UAUU": {"name": "Костанай (Kostanay)", "city": "Костанай"},
    "UAOO": {"name": "Кызылорда (Kyzylorda)", "city": "Кызылорда"},
    "UAOL": {"name": "Байконур (Baikonur)", "city": "Байконур"},
    "UATE": {"name": "Актау (Aktau)", "city": "Актау"},
    "UASP": {"name": "Павлодар (Pavlodar)", "city": "Павлодар"},
    "UACP": {"name": "Петропавловск (Petropavlovsk)", "city": "Петропавловск"},
    "UAII": {"name": "Шымкент (Shymkent)", "city": "Шымкент"},
    "UATA": {"name": "Туркестан (Turkistan)", "city": "Туркестан"},
    "UASK": {"name": "Усть-Каменогорск (Oskemen)", "city": "Усть-Каменогорск"},
    "UASB": {"name": "Семей (Semey)", "city": "Семей"},
}

# FIR Казахстана
KAZAKHSTAN_FIR = "UACC"


# ========================================
# NOTAM КОЛЛЕКТОР
# ========================================

class NOTAMCollector:
    """Сборщик NOTAM из источников Казаэронавигации"""
    
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (NOTAM Collector for Aviation)'
        })
    
    def fetch_all_notams(self) -> Dict:
        """
        Получение всех NOTAM по Казахстану
        
        Returns:
            Словарь с данными NOTAM
        """
        print("╔════════════════════════════════════════════════════════════╗")
        print("║  🛫 Сбор NOTAM для аэродромов Казахстана                   ║")
        print("╚════════════════════════════════════════════════════════════╝")
        print()
        
        results = {
            'source': None,
            'collection_time': datetime.utcnow().isoformat() + 'Z',
            'fir': KAZAKHSTAN_FIR,
            'total_notams': 0,
            'notams': []
        }
        
        # Попытка парсинга из основных источников с fallback
        sources = [
            ('notam.ans.kz', self._parse_notam_subdomain),
            ('ans.kz/ru/ais/notam', self._parse_main_site),
        ]
        
        for source_name, parse_func in sources:
            print(f"🔍 Попытка получить данные из {source_name}...")
            try:
                notams = parse_func()
                if notams:
                    results['source'] = source_name
                    results['notams'] = notams
                    results['total_notams'] = len(notams)
                    print(f"✅ Успешно получено {len(notams)} NOTAM из {source_name}")
                    break
                else:
                    print(f"⚠️  {source_name} не вернул данных")
            except Exception as e:
                print(f"❌ Ошибка при обращении к {source_name}: {e}")
            
            # Задержка между запросами
            time.sleep(1)
        
        if not results['notams']:
            print("\n❌ Не удалось получить NOTAM ни из одного источника")
        
        return results
    
    def fetch_by_airport(self, icao: str) -> Dict:
        """
        Получение NOTAM для конкретного аэродрома
        
        Args:
            icao: ICAO код аэродрома
            
        Returns:
            Словарь с данными NOTAM
        """
        icao = icao.upper()
        
        if icao not in AIRPORTS:
            raise ValueError(f"Аэродром {icao} не найден в базе Казахстана")
        
        print(f"🔍 Получение NOTAM для {icao} - {AIRPORTS[icao]['name']}")
        
        # Получаем все NOTAM и фильтруем
        all_data = self.fetch_all_notams()
        
        # Фильтрация по аэродрому
        filtered = [n for n in all_data['notams'] if n.get('location') == icao]
        
        result = {
            'source': all_data['source'],
            'collection_time': all_data['collection_time'],
            'fir': all_data['fir'],
            'airport': icao,
            'airport_name': AIRPORTS[icao]['name'],
            'total_notams': len(filtered),
            'notams': filtered
        }
        
        print(f"✅ Найдено {len(filtered)} NOTAM для {icao}")
        
        return result
    
    def _parse_notam_subdomain(self) -> List[Dict]:
        """
        Парсинг NOTAM с поддомена notam.ans.kz
        
        Returns:
            Список словарей с данными NOTAM
        """
        notams = []
        
        # Пробуем получить страницу со списком NOTAM
        urls = [
            'https://notam.ans.kz/notam/',
            'https://notam.ans.kz/notam_en/',
        ]
        
        for url in urls:
            try:
                response = self.session.get(url, timeout=10)
                if response.status_code == 200:
                    soup = BeautifulSoup(response.text, 'lxml')
                    
                    # Извлекаем блоки NOTAM
                    notam_blocks = self._extract_notam_blocks(soup)
                    
                    for block in notam_blocks:
                        parsed = self._parse_raw_notam(block)
                        if parsed:
                            notams.append(parsed)
                    
                    if notams:
                        return notams
                        
            except Exception as e:
                print(f"  ⚠️  Ошибка при парсинге {url}: {e}")
                continue
        
        return notams
    
    def _parse_main_site(self) -> List[Dict]:
        """
        Парсинг NOTAM с основного сайта ans.kz
        
        Returns:
            Список словарей с данными NOTAM
        """
        notams = []
        
        urls = [
            'https://www.ans.kz/ru/ais/notam',
            'https://www.ans.kz/en/ais/notam',
        ]
        
        for url in urls:
            try:
                response = self.session.get(url, timeout=10)
                if response.status_code == 200:
                    soup = BeautifulSoup(response.text, 'lxml')
                    
                    # Проверяем наличие iframe
                    iframe = soup.find('iframe')
                    if iframe and iframe.get('src'):
                        iframe_url = iframe['src']
                        if not iframe_url.startswith('http'):
                            iframe_url = 'https://www.ans.kz' + iframe_url
                        
                        # Получаем содержимое iframe
                        iframe_response = self.session.get(iframe_url, timeout=10)
                        if iframe_response.status_code == 200:
                            soup = BeautifulSoup(iframe_response.text, 'lxml')
                    
                    # Извлекаем блоки NOTAM
                    notam_blocks = self._extract_notam_blocks(soup)
                    
                    for block in notam_blocks:
                        parsed = self._parse_raw_notam(block)
                        if parsed:
                            notams.append(parsed)
                    
                    if notams:
                        return notams
                        
            except Exception as e:
                print(f"  ⚠️  Ошибка при парсинге {url}: {e}")
                continue
        
        return notams
    
    def _extract_notam_blocks(self, soup: BeautifulSoup) -> List[str]:
        """
        Универсальное извлечение блоков NOTAM из HTML
        
        Args:
            soup: BeautifulSoup объект
            
        Returns:
            Список текстов NOTAM
        """
        blocks = []
        
        # Попытка 1: Ищем в <pre> тегах
        pre_tags = soup.find_all('pre')
        for pre in pre_tags:
            text = pre.get_text(strip=False)
            if self._is_notam_text(text):
                blocks.append(text)
        
        # Попытка 2: Ищем в div с классами, связанными с NOTAM
        divs = soup.find_all('div', class_=re.compile(r'notam|bulletin', re.I))
        for div in divs:
            text = div.get_text(strip=False)
            if self._is_notam_text(text):
                blocks.append(text)
        
        # Попытка 3: Ищем в таблицах
        tables = soup.find_all('table')
        for table in tables:
            text = table.get_text(strip=False)
            if self._is_notam_text(text):
                blocks.append(text)
        
        # Попытка 4: Полнотекстовый поиск и разбиение
        if not blocks:
            full_text = soup.get_text()
            # Ищем паттерны NOTAM (начинаются с A****/**) 
            notam_pattern = r'([A-Z]\d{4}/\d{2}\s+NOTAM[NRC].*?)(?=[A-Z]\d{4}/\d{2}\s+NOTAM|$)'
            matches = re.finditer(notam_pattern, full_text, re.DOTALL)
            for match in matches:
                blocks.append(match.group(1))
        
        return blocks
    
    def _is_notam_text(self, text: str) -> bool:
        """
        Проверка, является ли текст NOTAM
        
        Args:
            text: Текст для проверки
            
        Returns:
            True если текст похож на NOTAM
        """
        # Проверяем наличие ключевых маркеров NOTAM
        markers = [
            r'[A-Z]\d{4}/\d{2}\s+NOTAM',
            r'Q\)\s*[A-Z]{4}/Q[A-Z]{4}',
            r'A\)\s*UA[A-Z]{2}',
        ]
        
        for marker in markers:
            if re.search(marker, text):
                return True
        
        return False
    
    def _parse_raw_notam(self, text: str) -> Optional[Dict]:
        """
        Парсинг сырого текста NOTAM
        
        Args:
            text: Сырой текст NOTAM
            
        Returns:
            Словарь с разобранными полями NOTAM
        """
        try:
            # Номер и тип NOTAM
            id_match = re.search(r'([A-Z]\d{4}/\d{2})\s+NOTAM([NRC])', text)
            if not id_match:
                return None
            
            notam_id = id_match.group(1)
            notam_type = id_match.group(2)
            
            # Q-строка
            q_match = re.search(
                r'Q\)\s*([A-Z]{4})/(Q[A-Z]{4})/([IV]{1,2})/([A-Z]+)/([AEW]+)/(\d{3})/(\d{3})/(\d{4}[NS]\d{5}[EW])(\d{3})?',
                text
            )
            
            q_data = {}
            if q_match:
                q_data = {
                    'fir': q_match.group(1),
                    'code': q_match.group(2),
                    'traffic': q_match.group(3),
                    'purpose': q_match.group(4),
                    'scope': q_match.group(5),
                    'lower': q_match.group(6),
                    'upper': q_match.group(7),
                    'coordinates': q_match.group(8),
                    'radius': q_match.group(9) if q_match.group(9) else None,
                }
            
            # A) Аэродром/FIR
            location_match = re.search(r'A\)\s*([A-Z]{4})', text)
            location = location_match.group(1) if location_match else q_data.get('fir')
            
            # B) Начало действия
            b_match = re.search(r'B\)\s*(\d{10,12})', text)
            valid_from = self._parse_notam_datetime(b_match.group(1)) if b_match else None
            
            # C) Окончание действия
            c_match = re.search(r'C\)\s*(\d{10,12}|PERM|EST)', text)
            valid_to = None
            is_permanent = False
            if c_match:
                c_value = c_match.group(1)
                if c_value == 'PERM':
                    is_permanent = True
                elif c_value == 'EST':
                    valid_to = 'EST'
                else:
                    valid_to = self._parse_notam_datetime(c_value)
            
            # D) Расписание
            d_match = re.search(r'D\)\s*([^\n]+)', text)
            schedule = d_match.group(1).strip() if d_match else None
            
            # E) Описание
            e_match = re.search(r'E\)\s*(.+?)(?=\s*[FG]\)|$)', text, re.DOTALL)
            description = e_match.group(1).strip() if e_match else None
            
            # F) Нижний предел
            f_match = re.search(r'F\)\s*([^\n]+)', text)
            lower_limit = f_match.group(1).strip() if f_match else None
            
            # G) Верхний предел
            g_match = re.search(r'G\)\s*([^\n]+)', text)
            upper_limit = g_match.group(1).strip() if g_match else None
            
            # Формируем результат
            notam = {
                'id': notam_id,
                'type': notam_type,
                'q_code': q_data.get('code'),
                'location': location,
                'valid_from': valid_from,
                'valid_to': valid_to,
                'is_permanent': is_permanent,
                'schedule': schedule,
                'description_raw': description,
                'lower_limit': lower_limit,
                'upper_limit': upper_limit,
                'q_data': q_data,
                'raw': text.strip()
            }
            
            return notam
            
        except Exception as e:
            print(f"  ⚠️  Ошибка парсинга NOTAM: {e}")
            return None
    
    def _parse_notam_datetime(self, dt_str: str) -> Optional[str]:
        """
        Парсинг даты/времени NOTAM (формат YYMMDDHHmm)
        
        Args:
            dt_str: Строка даты/времени
            
        Returns:
            ISO формат даты/времени
        """
        try:
            if len(dt_str) == 10:
                # YYMMDDHHmm
                year = int(dt_str[0:2])
                # Определяем век: если год > текущий год + 5, считаем что это прошлый век
                current_year = datetime.now().year % 100
                if year > current_year + 5:
                    year = 1900 + year
                else:
                    year = 2000 + year
                
                month = int(dt_str[2:4])
                day = int(dt_str[4:6])
                hour = int(dt_str[6:8])
                minute = int(dt_str[8:10])
                
                dt = datetime(year, month, day, hour, minute)
                return dt.isoformat() + 'Z'
            elif len(dt_str) == 12:
                # YYYYMMDDHHmm
                year = int(dt_str[0:4])
                month = int(dt_str[4:6])
                day = int(dt_str[6:8])
                hour = int(dt_str[8:10])
                minute = int(dt_str[10:12])
                
                dt = datetime(year, month, day, hour, minute)
                return dt.isoformat() + 'Z'
        except Exception:
            pass
        
        return None


# ========================================
# CLI
# ========================================

def main():
    parser = argparse.ArgumentParser(
        description='Сбор NOTAM для аэродромов Казахстана',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Примеры использования:

  # Все NOTAM по Казахстану
  python3 collect_notam.py
  
  # NOTAM для конкретного аэродрома
  python3 collect_notam.py --icao UAAA
  
  # Сохранение в файл
  python3 collect_notam.py --output notam_data.json
  
  # Список аэродромов
  python3 collect_notam.py --list-airports
        """
    )
    
    parser.add_argument(
        '--icao',
        type=str,
        help='ICAO код аэродрома'
    )
    
    parser.add_argument(
        '--output', '-o',
        type=str,
        help='Путь к файлу для сохранения результата (JSON)'
    )
    
    parser.add_argument(
        '--list-airports',
        action='store_true',
        help='Показать список аэродромов Казахстана'
    )
    
    args = parser.parse_args()
    
    # Список аэродромов
    if args.list_airports:
        print("\n╔════════════════════════════════════════════════════════════╗")
        print("║  📋 Аэродромы Казахстана                                   ║")
        print("╚════════════════════════════════════════════════════════════╝")
        print()
        for icao, info in sorted(AIRPORTS.items()):
            print(f"  {icao} - {info['name']}")
        print()
        return
    
    # Сбор NOTAM
    collector = NOTAMCollector()
    
    if args.icao:
        # По конкретному аэродрому
        try:
            results = collector.fetch_by_airport(args.icao)
        except ValueError as e:
            print(f"❌ {e}")
            return
    else:
        # Все NOTAM
        results = collector.fetch_all_notams()
    
    # Вывод итогов
    print()
    print("╔════════════════════════════════════════════════════════════╗")
    print("║  📊 Итоги сбора                                            ║")
    print("╚════════════════════════════════════════════════════════════╝")
    print()
    print(f"Источник:        {results.get('source', 'N/A')}")
    print(f"FIR:             {results.get('fir', 'N/A')}")
    print(f"Всего NOTAM:     {results.get('total_notams', 0)}")
    print(f"Время сбора:     {results.get('collection_time', 'N/A')}")
    print()
    
    # Краткий вывод NOTAM
    if results.get('notams'):
        print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        print("📋 NOTAM:")
        print("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        for notam in results['notams']:
            print(f"\n{notam['id']} ({notam['type']}) - {notam['location']}")
            if notam.get('description_raw'):
                desc = notam['description_raw'][:80] + '...' if len(notam['description_raw']) > 80 else notam['description_raw']
                print(f"  {desc}")
            print(f"  Действует: {notam.get('valid_from', 'N/A')} - {notam.get('valid_to', 'N/A')}")
    
    # Сохранение в файл
    if args.output:
        with open(args.output, 'w', encoding='utf-8') as f:
            json.dump(results, f, ensure_ascii=False, indent=2)
        print(f"\n💾 Данные сохранены в: {args.output}")


if __name__ == '__main__':
    main()

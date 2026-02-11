#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
🛫 Сбор TAF (Terminal Aerodrome Forecast) для аэродромов
Казахстана и соседних стран

Использование:
    python3 collect_taf.py
    python3 collect_taf.py --country KZ
    python3 collect_taf.py --output taf_data.json
"""

import requests
import json
import argparse
from datetime import datetime
from typing import List, Dict, Optional
import time

# ========================================
# БАЗА ДАННЫХ АЭРОДРОМОВ
# ========================================

AIRPORTS = {
    # КАЗАХСТАН
    "KZ": [
        # Алматинская область
        {"icao": "UAAA", "iata": "ALA", "name": "Алматы (Almaty)", "city": "Алматы"},
        {"icao": "UAAT", "iata": "TDK", "name": "Талдыкорган (Taldykorgan)", "city": "Талдыкорган"},
        
        # Астана
        {"icao": "UACC", "iata": "NQZ", "name": "Астана (Nursultan Nazarbayev)", "city": "Астана"},
        
        # Актюбинская область
        {"icao": "UATT", "iata": "AKX", "name": "Актобе (Aktobe)", "city": "Актобе"},
        
        # Атырауская область
        {"icao": "UATG", "iata": "GUW", "name": "Атырау (Atyrau)", "city": "Атырау"},
        
        # Западно-Казахстанская область
        {"icao": "UARR", "iata": "URA", "name": "Уральск (Oral/Uralsk)", "city": "Уральск"},
        
        # Карагандинская область
        {"icao": "UAKK", "iata": "KGF", "name": "Караганда (Sary-Arka)", "city": "Караганда"},
        {"icao": "UAAH", "iata": "BXH", "name": "Балхаш (Balkhash)", "city": "Балхаш"},
        
        # Костанайская область
        {"icao": "UAUU", "iata": "KSN", "name": "Костанай (Kostanay)", "city": "Костанай"},
        
        # Кызылординская область
        {"icao": "UAOO", "iata": "KZO", "name": "Кызылорда (Kyzylorda)", "city": "Кызылорда"},
        {"icao": "UAOL", "iata": "BAY", "name": "Байконур (Baikonur)", "city": "Байконур"},
        
        # Мангистауская область
        {"icao": "UATE", "iata": "SCO", "name": "Актау (Aktau)", "city": "Актау"},
        
        # Павлодарская область
        {"icao": "UASP", "iata": "PWQ", "name": "Павлодар (Pavlodar)", "city": "Павлодар"},
        
        # Северо-Казахстанская область
        {"icao": "UACP", "iata": "PPK", "name": "Петропавловск (Petropavlovsk)", "city": "Петропавловск"},
        
        # Туркестанская область
        {"icao": "UAII", "iata": "CIT", "name": "Шымкент (Shymkent)", "city": "Шымкент"},
        {"icao": "UATA", "iata": "HSA", "name": "Туркестан (Turkistan)", "city": "Туркестан"},
        
        # Восточно-Казахстанская область
        {"icao": "UASK", "iata": "UKK", "name": "Усть-Каменогорск (Oskemen)", "city": "Усть-Каменогорск"},
        {"icao": "UASB", "iata": "SZI", "name": "Семей (Semey)", "city": "Семей"},
        
        # Жамбылская область
        {"icao": "UAAH", "iata": "DMB", "name": "Тараз (Taraz)", "city": "Тараз"},
    ],
    
    # РОССИЯ (граничные регионы)
    "RU": [
        # Омская область
        {"icao": "UNOO", "iata": "OMS", "name": "Омск (Omsk)", "city": "Омск"},
        
        # Новосибирская область
        {"icao": "UNNT", "iata": "OVB", "name": "Новосибирск (Tolmachevo)", "city": "Новосибирск"},
        
        # Алтайский край
        {"icao": "UNBB", "iata": "BAX", "name": "Барнаул (Barnaul)", "city": "Барнаул"},
        
        # Астраханская область
        {"icao": "URWA", "iata": "ASF", "name": "Астрахань (Astrakhan)", "city": "Астрахань"},
        
        # Волгоградская область
        {"icao": "URWW", "iata": "VOG", "name": "Волгоград (Volgograd)", "city": "Волгоград"},
        
        # Саратовская область
        {"icao": "UWSS", "iata": "RTW", "name": "Саратов (Saratov)", "city": "Саратов"},
        
        # Самарская область
        {"icao": "UWWW", "iata": "KUF", "name": "Самара (Kurumoch)", "city": "Самара"},
        
        # Оренбургская область
        {"icao": "UWOO", "iata": "REN", "name": "Оренбург (Orenburg)", "city": "Оренбург"},
        
        # Челябинская область
        {"icao": "USCC", "iata": "CEK", "name": "Челябинск (Chelyabinsk)", "city": "Челябинск"},
        
        # Курганская область
        {"icao": "USUU", "iata": "KRO", "name": "Курган (Kurgan)", "city": "Курган"},
        
        # Тюменская область
        {"icao": "USTR", "iata": "TJM", "name": "Тюмень (Tyumen)", "city": "Тюмень"},
    ],
    
    # УЗБЕКИСТАН
    "UZ": [
        {"icao": "UTTT", "iata": "TAS", "name": "Ташкент (Tashkent)", "city": "Ташкент"},
        {"icao": "UTNU", "iata": "NMA", "name": "Намangan (Namangan)", "city": "Намаган"},
        {"icao": "UTFA", "iata": "FEG", "name": "Фергана (Fergana)", "city": "Фергана"},
        {"icao": "UTSS", "iata": "SKD", "name": "Самарканд (Samarkand)", "city": "Самарканд"},
        {"icao": "UTSB", "iata": "BHK", "name": "Бухара (Bukhara)", "city": "Бухара"},
        {"icao": "UTNN", "iata": "UGC", "name": "Ургенч (Urgench)", "city": "Ургенч"},
        {"icao": "UTSA", "iata": "AZN", "name": "Андижан (Andijan)", "city": "Андижан"},
    ],
    
    # КЫРГЫЗСТАН
    "KG": [
        {"icao": "UAFM", "iata": "FRU", "name": "Бишкек (Manas)", "city": "Бишкек"},
        {"icao": "UCFM", "iata": "OSS", "name": "Ош (Osh)", "city": "Ош"},
        {"icao": "UCFI", "iata": "IKU", "name": "Иссык-Куль (Issyk-Kul)", "city": "Иссык-Куль"},
    ],
    
    # ТУРКМЕНИСТАН
    "TM": [
        {"icao": "UTAA", "iata": "ASB", "name": "Ашхабад (Ashgabat)", "city": "Ашхабад"},
        {"icao": "UTAK", "iata": "KRW", "name": "Туркменабат (Turkmenbashi)", "city": "Туркменабат"},
        {"icao": "UTAV", "iata": "TAZ", "name": "Дашогуз (Dashoguz)", "city": "Дашогуз"},
    ],
    
    # КИТАЙ (Синьцзян)
    "CN": [
        {"icao": "ZWWW", "iata": "URC", "name": "Урумчи (Urumqi)", "city": "Урумчи"},
        {"icao": "ZWKL", "iata": "KRL", "name": "Коргас (Korla)", "city": "Коргас"},
        {"icao": "ZWAT", "iata": "AAT", "name": "Алтай (Altay)", "city": "Алтай"},
    ],
}


# ========================================
# API ИСТОЧНИКИ
# ========================================

class TAFCollector:
    """Сборщик данных TAF из различных источников"""
    
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({
            'User-Agent': 'Mozilla/5.0 (TAF Collector for Aviation)'
        })
    
    def get_taf_aviationweather(self, icao: str) -> Optional[Dict]:
        """
        Получение TAF из Aviation Weather Center (NOAA)
        https://aviationweather.gov
        """
        try:
            url = f"https://aviationweather.gov/api/data/taf?ids={icao}&format=json"
            response = self.session.get(url, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                if data:
                    return {
                        'source': 'aviationweather.gov',
                        'raw': data[0].get('rawTAF', ''),
                        'issue_time': data[0].get('issueTime', ''),
                        'valid_time_from': data[0].get('validTimeFrom', ''),
                        'valid_time_to': data[0].get('validTimeTo', ''),
                        'data': data[0]
                    }
        except Exception as e:
            print(f"  ⚠️  aviationweather.gov error: {e}")
        
        return None
    
    def get_taf_checkwx(self, icao: str, api_key: Optional[str] = None) -> Optional[Dict]:
        """
        Получение TAF из CheckWX API
        https://www.checkwx.com
        
        Требует API ключ (бесплатный: до 1000 запросов/день)
        """
        if not api_key:
            return None
        
        try:
            url = f"https://api.checkwx.com/taf/{icao}/decoded"
            headers = {'X-API-Key': api_key}
            response = self.session.get(url, headers=headers, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                if data.get('data'):
                    return {
                        'source': 'checkwx.com',
                        'raw': data['data'][0].get('raw_text', ''),
                        'decoded': data['data'][0],
                        'data': data['data'][0]
                    }
        except Exception as e:
            print(f"  ⚠️  checkwx.com error: {e}")
        
        return None
    
    def get_taf_avwx(self, icao: str) -> Optional[Dict]:
        """
        Получение TAF из AVWX API
        https://avwx.rest
        """
        try:
            url = f"https://avwx.rest/api/taf/{icao}"
            response = self.session.get(url, timeout=10)
            
            if response.status_code == 200:
                data = response.json()
                return {
                    'source': 'avwx.rest',
                    'raw': data.get('raw', ''),
                    'decoded': data,
                    'data': data
                }
        except Exception as e:
            print(f"  ⚠️  avwx.rest error: {e}")
        
        return None
    
    def get_taf_ogimet(self, icao: str) -> Optional[Dict]:
        """
        Получение TAF из Ogimet
        https://www.ogimet.com
        """
        try:
            url = f"https://www.ogimet.com/display_taf.php?lang=en&place={icao}&type=ALL"
            response = self.session.get(url, timeout=10)
            
            if response.status_code == 200 and 'TAF' in response.text:
                # Простой парсинг HTML
                text = response.text
                if 'class="taf"' in text or 'TAF ' + icao in text:
                    # Ищем TAF в тексте
                    start = text.find(f'TAF {icao}')
                    if start != -1:
                        end = text.find('=', start)
                        if end != -1:
                            raw_taf = text[start:end+1].strip()
                            return {
                                'source': 'ogimet.com',
                                'raw': raw_taf,
                                'data': {'raw_text': raw_taf}
                            }
        except Exception as e:
            print(f"  ⚠️  ogimet.com error: {e}")
        
        return None
    
    def collect(self, icao: str, checkwx_api_key: Optional[str] = None) -> Optional[Dict]:
        """
        Сбор TAF из всех доступных источников (приоритет)
        """
        sources = [
            ('aviationweather', lambda: self.get_taf_aviationweather(icao)),
            ('avwx', lambda: self.get_taf_avwx(icao)),
        ]
        
        if checkwx_api_key:
            sources.insert(0, ('checkwx', lambda: self.get_taf_checkwx(icao, checkwx_api_key)))
        
        for source_name, func in sources:
            try:
                result = func()
                if result and result.get('raw'):
                    return result
                time.sleep(0.5)  # Небольшая задержка между запросами
            except Exception as e:
                print(f"  ❌ {source_name} failed: {e}")
                continue
        
        return None


# ========================================
# ОСНОВНОЙ СКРИПТ
# ========================================

def collect_all_taf(
    countries: Optional[List[str]] = None,
    checkwx_api_key: Optional[str] = None,
    output_file: Optional[str] = None
) -> Dict:
    """
    Сбор TAF для всех аэродромов
    
    Args:
        countries: Список кодов стран (None = все)
        checkwx_api_key: API ключ для CheckWX (опционально)
        output_file: Путь к файлу для сохранения результата
    
    Returns:
        Словарь с данными TAF
    """
    
    if countries is None:
        countries = list(AIRPORTS.keys())
    
    collector = TAFCollector()
    results = {
        'collection_time': datetime.utcnow().isoformat() + 'Z',
        'total_airports': 0,
        'successful': 0,
        'failed': 0,
        'countries': {},
        'taf_data': []
    }
    
    print("╔════════════════════════════════════════════════════════════╗")
    print("║  🛫 Сбор TAF для аэродромов                                ║")
    print("╚════════════════════════════════════════════════════════════╝")
    print()
    
    for country_code in countries:
        if country_code not in AIRPORTS:
            print(f"⚠️  Страна {country_code} не найдена в базе")
            continue
        
        airports = AIRPORTS[country_code]
        country_name = {
            'KZ': 'Казахстан',
            'RU': 'Россия',
            'UZ': 'Узбекистан',
            'KG': 'Кыргызстан',
            'TM': 'Туркменистан',
            'CN': 'Китай'
        }.get(country_code, country_code)
        
        print(f"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        print(f"🌍 {country_name} ({country_code}) — {len(airports)} аэродромов")
        print(f"━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        
        country_stats = {
            'total': len(airports),
            'successful': 0,
            'failed': 0,
            'airports': []
        }
        
        for airport in airports:
            icao = airport['icao']
            name = airport['name']
            city = airport['city']
            
            results['total_airports'] += 1
            
            print(f"\n📍 {icao} - {name} ({city})")
            print(f"   Запрос TAF...", end=' ')
            
            taf_data = collector.collect(icao, checkwx_api_key)
            
            if taf_data:
                print(f"✅ Получено от {taf_data['source']}")
                
                # Показываем первые 100 символов
                raw_preview = taf_data['raw'][:100] + '...' if len(taf_data['raw']) > 100 else taf_data['raw']
                print(f"   📄 {raw_preview}")
                
                results['successful'] += 1
                country_stats['successful'] += 1
                
                # Сохраняем данные
                airport_data = {
                    **airport,
                    'taf': taf_data,
                    'collection_time': datetime.utcnow().isoformat() + 'Z'
                }
                results['taf_data'].append(airport_data)
                country_stats['airports'].append(airport_data)
                
            else:
                print("❌ Не удалось получить")
                results['failed'] += 1
                country_stats['failed'] += 1
            
            # Задержка между запросами
            time.sleep(1)
        
        results['countries'][country_code] = country_stats
        print()
    
    # Итоги
    print("╔════════════════════════════════════════════════════════════╗")
    print("║  📊 Итоги сбора                                            ║")
    print("╚════════════════════════════════════════════════════════════╝")
    print()
    print(f"Всего аэродромов:  {results['total_airports']}")
    print(f"✅ Успешно:         {results['successful']}")
    print(f"❌ Не удалось:      {results['failed']}")
    print(f"📈 Процент успеха:  {results['successful'] / results['total_airports'] * 100:.1f}%")
    print()
    
    # Сохранение в файл
    if output_file:
        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(results, f, ensure_ascii=False, indent=2)
        print(f"💾 Данные сохранены в: {output_file}")
    
    return results


# ========================================
# CLI
# ========================================

def main():
    parser = argparse.ArgumentParser(
        description='Сбор TAF для аэродромов Казахстана и соседних стран',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Примеры использования:
  
  # Все аэродромы всех стран
  python3 collect_taf.py
  
  # Только Казахстан
  python3 collect_taf.py --country KZ
  
  # Казахстан и Россия
  python3 collect_taf.py --country KZ RU
  
  # С API ключом CheckWX
  python3 collect_taf.py --checkwx-key YOUR_API_KEY
  
  # Сохранение в файл
  python3 collect_taf.py --output taf_data.json
  
  # Список доступных стран
  python3 collect_taf.py --list-countries
        """
    )
    
    parser.add_argument(
        '--country',
        nargs='+',
        choices=['KZ', 'RU', 'UZ', 'KG', 'TM', 'CN'],
        help='Коды стран для сбора (по умолчанию: все)'
    )
    
    parser.add_argument(
        '--output', '-o',
        type=str,
        help='Путь к файлу для сохранения результата (JSON)'
    )
    
    parser.add_argument(
        '--checkwx-key',
        type=str,
        help='API ключ для CheckWX (опционально, увеличивает успешность)'
    )
    
    parser.add_argument(
        '--list-countries',
        action='store_true',
        help='Показать список доступных стран'
    )
    
    parser.add_argument(
        '--list-airports',
        type=str,
        choices=['KZ', 'RU', 'UZ', 'KG', 'TM', 'CN'],
        help='Показать список аэродромов для страны'
    )
    
    args = parser.parse_args()
    
    # Список стран
    if args.list_countries:
        print("\n📋 Доступные страны:\n")
        for code, airports in AIRPORTS.items():
            country_name = {
                'KZ': 'Казахстан',
                'RU': 'Россия',
                'UZ': 'Узбекистан',
                'KG': 'Кыргызстан',
                'TM': 'Туркменистан',
                'CN': 'Китай'
            }.get(code, code)
            print(f"  {code} - {country_name} ({len(airports)} аэродромов)")
        print()
        return
    
    # Список аэродромов
    if args.list_airports:
        code = args.list_airports
        print(f"\n📋 Аэродромы ({code}):\n")
        for airport in AIRPORTS[code]:
            print(f"  {airport['icao']} ({airport['iata']}) - {airport['name']}")
        print()
        return
    
    # Сбор данных
    collect_all_taf(
        countries=args.country,
        checkwx_api_key=args.checkwx_key,
        output_file=args.output
    )


if __name__ == '__main__':
    main()
